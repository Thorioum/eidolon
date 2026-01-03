

<p align="center">
  <img width="128" height="128" align="center" alt="app-icon" src="https://github.com/user-attachments/assets/21440f65-68a1-4af7-a411-bd34555d47c4" /></br>
  Java interface for converting any audio into a playable representation of Minecraft sound effects.
</p>

# How the Program Works

- Run the standalone jar, choose an input audio, choose a version you want to make the datapack for, and hit Run.

- The program dynamically downloads the sound assets from the internet when you hit run—meaning if you don't have the version cached yet, you need an internet connection.

- Then for every existing Minecraft sound effect, the program builds an index of a [frame length](#Frame) of its raw audio at [various pitches](#Pitches) ranging from 0.5 to 2.0.

- With the complete index, it will split the input audio into chunks of length [you specified](#Frame), and begin matching each frame of audio to the big index of pitch shifted minecraft sound effects.

- After matching up to the amount specified in [Sounds Per Frame](#Sounds) it will move onto the next frame and calculate the next combination.

- At any time during the matching process you can hit "Save" to save the amount it has processed into a datapack. This datapack can be added to any world, making the transcribed song playable through /function.</br>

- During the matching process there is also a "Listen" button that attempts to simulate how your currently processed audio will sound like ingame if you were to play it.

- The transcription is playable through running /playsound on all the calculated sounds for a frame, and then right before the next one, all the sounds are stopped with /stopsound, and the next frame sound composition is played.

### REMEMBER

- **Remember to set the tickrate through /tick rate when you run the datapack, as datapacks can't run /tick rate. If your frame length is a multiple of 50, the default tick rate at 20 works fine. If your frame length is not a multiple of 50, your tick rate should be 100.**

# Important Facts

The Minecraft sound system has a few quirks that are important to keep in mind. 

- The maximum number of sounds that can be played at once is about 247.

- One normal tick is 50 ms. A normal tick rate is 20 ticks per second.

- /stopsound can only be used once per server tick, but you can increase the frequency you can run it by increasing the server tick rate with /tick.<br/>

  Because of the fact above, on versions 23w43a (1.20.3 snapshot) and below, where /tick is unavailable, you cannot have a framelength that isnt a multiple of 50.

- Although /stopsound can be used more than one time per client tick with /tick as it relies on server tick rate, the client counter for how many sounds are playing is reset every client tick - regardless of what /tick is set to.<br/>

  Due to the fact above, if you play 60 sounds, /stopsound, and play 60 more sounds all in the same client tick, the calculated sounds playing will be 120, because your client tick has not ended.

  Keeping this in mind, you need to lower the amount of sounds per tick if you also lower the frame rate.

  This quirk can be mitigated with a client hack like "Timer", which speeds up the client tick rate. Asking people to use it just to listen seems a bit unreasonable though, don't you think.

- Datapacks were added in version 1.13, and the schedule command in 1.14. If you wanted to make a playable transcription for a version earlier than 1.14, datapacks aren't really going to work. </br>

- A suggestion is making a client-side mod to automate executing the playsound commands through command blocks.

# Sound Settings

## Sounds Per Frame

- This is the amount of sounds that are going to be used to make up a frame of audio. The more sounds you allow here, the more of the full audio gets represented in the final product.
The higher this is, the greater the accuracy of the transcription. Increases processing time linearly

---

## Frame Length

- This is how long a frame of audio is after the input is chopped into them. If you have the option, the lower this number, the better, as the shorter the frame, the easier it is to match accurate sounds to it.
Typically make this as small as you can. If you can use /tick, try 20; if you can't, use 50. Read some important facts above about choosing this number and sounds per frame effectively. Negligible effect on processing time.

---

## Pitches Per Sound

- When creating an index of sounds in order to use to match with a frame of audio, a sound is calculated at a number of pitches (this number) ranging from the playable range of 0.5 to 2.0. The higher this number is, the more options the matcher
gets when attempting to choose the most accurate sound effect. The higher the number, typically the higher the quality, though quality gain is negligible after a certain point. 128 is recommended. Increases processing time.

---

## Highpass

- This number is used in order to try to take out deep bass out of your input audio. Typically the matcher has a hard time finding truly accurate sound effects to match deep bass, reducing quality when it tries too. Increasing this number will reduce the bass in the input audio.
No effect on processing time.

---

## Brightness

- Typically, because Minecraft sound effects are just normal sounds, they play audible tones at a wide variety of frequencies. This number is a filter for sound effects that have a large frequency range and gets stricter the closer you get to zero. Output may sound less cluttered but more '8-bit.'

---

# Program Preview and Recordings

<img width="949" height="568" alt="image" src="https://github.com/user-attachments/assets/45d4dfbf-47df-465e-b23c-f44cf4d5c114" />

## Recordings of transcribed datapacks.

- Some will be linked in a samples folder in this repo<br/>

https://github.com/user-attachments/assets/a82bb61a-a44c-40dd-bf79-8c8ccbc73511

https://github.com/user-attachments/assets/d90685e2-2e26-48e2-9b0e-57f85f648514

It's clear to see which is better in the examples given above. The first one, being frame length 20, is only possible because of /tick rate.

---

https://github.com/user-attachments/assets/5f7f74f9-cf78-411e-9e7d-7a5a9508864a

