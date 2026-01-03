package net.thorioum.matchers;

import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.result.SingleSoundResult;
import net.thorioum.sound.SoundMatcher;
import org.jocl.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jocl.CL.*;

public class GreedySubGpuMatcher implements Matcher {

    private cl_context context;
    private cl_command_queue queue;
    private cl_program program;
    private cl_kernel kernelDotSim;

    private cl_mem dCandidates;
    private cl_mem dNorms;
    private cl_mem dResidual;
    private cl_mem dSims;
    private cl_mem dVols;

    private int frameSize;
    private int numCandidates;
    private float[] hSims;
    private float[] hVols;
    private CandidateMeta[] meta;
    private boolean ready = false;

    private final Object lock = new Object();

    static {
        CL.setExceptionsEnabled(true);
    }

    record CandidateMeta(String name, double pitch) {}

    private static final String KERNEL_SRC =
            "__kernel void dotSimVol(\n" +
                    "    __global const float* candidates,\n" +
                    "    __global const float* norms,\n" +
                    "    __global const float* residual,\n" +
                    "    const int frameSize,\n" +
                    "    const float residualNorm,\n" +
                    "    __global float* outSims,\n" +
                    "    __global float* outVols,\n" +
                    "    __local float* cache)\n" +
                    "{\n" +
                    "    int cid = get_group_id(0);\n" +
                    "    int lid = get_local_id(0);\n" +
                    "    int lsz = get_local_size(0);\n" +
                    "    float sum = 0.0f;\n" +
                    "    int base = cid * frameSize;\n" +
                    "    for (int i = lid; i < frameSize; i += lsz) {\n" +
                    "        sum += candidates[base + i] * residual[i];\n" +
                    "    }\n" +
                    "    cache[lid] = sum;\n" +
                    "    barrier(CLK_LOCAL_MEM_FENCE);\n" +
                    "    for (int offset = lsz >> 1; offset > 0; offset >>= 1) {\n" +
                    "        if (lid < offset) cache[lid] += cache[lid + offset];\n" +
                    "        barrier(CLK_LOCAL_MEM_FENCE);\n" +
                    "    }\n" +
                    "    if (lid == 0) {\n" +
                    "        float dot = cache[0];\n" +
                    "        float n   = fmax(norms[cid], 1e-20f);\n" +
                    "        float rn  = fmax(residualNorm, 1e-20f);\n" +
                    "        float cs  = dot / (n * rn);\n" +
                    "        if (cs < 0.0f) cs = 0.0f;\n" +
                    "        float sim = cs * n;\n" +
                    "        float vol = dot / (n*n + 1e-10f);\n" +
                    "        if (vol < 0.0f) vol = 0.0f;\n" +
                    "        if (vol > 1.0f) vol = 1.0f;\n" +
                    "        outSims[cid] = sim;\n" +
                    "        outVols[cid] = vol;\n" +
                    "    }\n" +
                    "}\n";

    @Override
    public boolean isReady() { return ready; }

    @Override
    public void buildFromDatabase(SoundEffectDatabase db, int frameSize, List<String> blacklistedSounds) {
        synchronized (lock) {
            release();

            this.frameSize = frameSize;
            List<float[]> rows = new ArrayList<>();
            List<Float> norms = new ArrayList<>();
            List<CandidateMeta> metas = new ArrayList<>();

            for (Map.Entry<String, Map<Double, double[]>> e : db.pitchShiftedEffects.entrySet()) {
                String name = e.getKey();
                if(blacklistedSounds.contains(name)) continue;

                Map<Double, double[]> pitches = e.getValue();
                Map<Double, Double> normMap = db.effectNorms.get(name);
                if (normMap == null) continue;

                for (Map.Entry<Double, double[]> p : pitches.entrySet()) {
                    double pitch = p.getKey();
                    double[] hp = p.getValue();
                    if (hp == null || hp.length != frameSize) continue;
                    Double n = normMap.get(pitch);
                    if (n == null || n <= 0) continue;

                    float[] row = new float[frameSize];
                    for (int i = 0; i < frameSize; i++) row[i] = (float) hp[i];
                    rows.add(row);
                    norms.add((float) (double) n);
                    metas.add(new CandidateMeta(name, pitch));
                }
            }

            this.numCandidates = rows.size();
            if (numCandidates == 0) { ready = false; return; }

            float[] candidateMatrix = new float[numCandidates * frameSize];
            for (int c = 0; c < numCandidates; c++) {
                System.arraycopy(rows.get(c), 0, candidateMatrix, c * frameSize, frameSize);
            }

            float[] normsArr = new float[numCandidates];
            for (int i = 0; i < numCandidates; i++) normsArr[i] = norms.get(i);

            this.meta  = metas.toArray(new CandidateMeta[0]);
            this.hSims = new float[numCandidates];
            this.hVols = new float[numCandidates];

            cl_platform_id[] platforms = new cl_platform_id[1];
            clGetPlatformIDs(1, platforms, null);
            cl_device_id device = pickDevice(platforms[0]);

            cl_context_properties props = new cl_context_properties();
            props.addProperty(CL_CONTEXT_PLATFORM, platforms[0]);

            context = clCreateContext(props, 1, new cl_device_id[]{device}, null, null, null);
            queue   = clCreateCommandQueue(context, device, 0, null);

            program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_SRC}, null, null);
            int err = clBuildProgram(program, 0, null, null, null, null);
            if (err != CL_SUCCESS) {
                long[] logSize = new long[1];
                clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, 0, null, logSize);
                byte[] logData = new byte[(int) logSize[0]];
                clGetProgramBuildInfo(program, device, CL_PROGRAM_BUILD_LOG, logSize[0], Pointer.to(logData), null);
                throw new RuntimeException("OpenCL build failed:\n" + new String(logData));
            }
            kernelDotSim = clCreateKernel(program, "dotSimVol", null);

            dCandidates = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) candidateMatrix.length * Sizeof.cl_float, Pointer.to(candidateMatrix), null);
            dNorms = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    (long) normsArr.length * Sizeof.cl_float, Pointer.to(normsArr), null);
            dResidual = clCreateBuffer(context, CL_MEM_READ_ONLY, (long) frameSize * Sizeof.cl_float, null, null);
            dSims = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) numCandidates * Sizeof.cl_float, null, null);
            dVols = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) numCandidates * Sizeof.cl_float, null, null);

            ready = true;
        }
    }

    @Override
    public SingleSoundResult findBestMatch(ConverterContext ctx, float[] residual, double residualEnergy, float minSim) {
        synchronized (lock) {
            if (!ready) return null;

            float residualNorm = (float) Math.sqrt(residualEnergy);
            clEnqueueWriteBuffer(queue, dResidual, CL_TRUE, 0L,
                    (long) frameSize * Sizeof.cl_float, Pointer.to(residual), 0, null, null);

            int arg = 0;
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_mem, Pointer.to(dCandidates));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_mem, Pointer.to(dNorms));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_mem, Pointer.to(dResidual));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_int, Pointer.to(new int[]{frameSize}));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_float, Pointer.to(new float[]{residualNorm}));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_mem, Pointer.to(dSims));
            clSetKernelArg(kernelDotSim, arg++, Sizeof.cl_mem, Pointer.to(dVols));

            int localSize = 256;
            clSetKernelArg(kernelDotSim, arg++, (long) localSize * Sizeof.cl_float, null);

            long[] global = new long[]{(long) numCandidates * localSize};
            long[] local  = new long[]{localSize};
            clEnqueueNDRangeKernel(queue, kernelDotSim, 1, null, global, local, 0, null, null);

            clEnqueueReadBuffer(queue, dSims, CL_TRUE, 0L,
                    (long) numCandidates * Sizeof.cl_float, Pointer.to(hSims), 0, null, null);
            clEnqueueReadBuffer(queue, dVols, CL_TRUE, 0L,
                    (long) numCandidates * Sizeof.cl_float, Pointer.to(hVols), 0, null, null);

            int best = -1;
            float bestSim = -Float.MAX_VALUE;
            for (int i = 0; i < numCandidates; i++) {
                float s = hSims[i];
                if (s > bestSim) { bestSim = s; best = i; }
            }
            if (best < 0 || bestSim < minSim) return null;

            CandidateMeta m = meta[best];
            double volume = hVols[best];
            return new SingleSoundResult(m.name, m.pitch, volume, bestSim, SoundMatcher.getDatabase(ctx).pitchShiftedEffects.get(m.name).get(m.pitch));
        }
    }

    public void release() {
        if (dCandidates != null) clReleaseMemObject(dCandidates);
        if (dNorms != null)      clReleaseMemObject(dNorms);
        if (dResidual != null)   clReleaseMemObject(dResidual);
        if (dSims != null)       clReleaseMemObject(dSims);
        if (dVols != null)       clReleaseMemObject(dVols);
        if (kernelDotSim != null) clReleaseKernel(kernelDotSim);
        if (program != null)      clReleaseProgram(program);
        if (queue != null)        clReleaseCommandQueue(queue);
        if (context != null)      clReleaseContext(context);
        dCandidates = dNorms = dResidual = dSims = dVols = null;
        kernelDotSim = null;
        program = null;
        queue = null;
        context = null;
        ready = false;
    }

    private static cl_device_id pickDevice(cl_platform_id platform) {
        cl_device_id[] devs = devices(platform, CL_DEVICE_TYPE_GPU);
        if (devs.length == 0) devs = devices(platform, CL_DEVICE_TYPE_CPU);
        if (devs.length == 0) throw new RuntimeException("No OpenCL device found");
        return devs[0];
    }

    private static cl_device_id[] devices(cl_platform_id platform, long type) {
        int[] num = new int[1];
        clGetDeviceIDs(platform, type, 0, null, num);
        if (num[0] == 0) return new cl_device_id[0];
        cl_device_id[] arr = new cl_device_id[num[0]];
        clGetDeviceIDs(platform, type, num[0], arr, null);
        return arr;
    }
}
