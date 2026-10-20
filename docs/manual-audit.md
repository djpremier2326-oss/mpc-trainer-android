# MPC Live III phone trainer: behavior and teaching audit

Reference: Akai MPC Live III User Guide, manual version 3.6:
https://cdn.inmusicbrands.com/akai/MPC%20Live%20III%20-%20User%20Guide%20-%203.6.pdf
Printed pages 72–77 (TC, performance features), 364–369 (hardware controls), and the Browser, Sample Edit and Step Sequencer chapters.

The trainer is an educational subset. This is a code/automated behavior audit, not a claim of acoustic or physical-device validation.

| Implemented area | Audit result / phone adaptation | Verification |
| --- | --- | --- |
| MAIN and project structure | Course explains sample, pad, track, sequence and project before actions. Four tracks share one pad pool; MPC 3 track types are not simulated. | Android lesson-dialog test; course content checks |
| BROWSE / PREVIEW | System file picker, streaming WAV/MP3 audition, explicit load to chosen pad. | Code review; audio files require device playback checks |
| Pads / Banks | A–D only; lessons explicitly distinguish full hardware A–H. No claim of phone pressure sensitivity. | State/persistence review; practice-kit integration test |
| REC | Arms recording; PLAY begins, with optional count-in. Track events are replaced only at actual recording start. | Android behavior test |
| PLAY / STOP | STOP retains position; PLAY resumes. Long-press PLAY supplies PLAY START as an explicit phone adaptation. | Android behavior test |
| OVERDUB | Can be enabled/disabled while playing; keeps existing events. REC automatically enters Overdub after the first loop. | Android behavior test; code review |
| Completed recording tasks | Only actual recorded hits set pass flags. STOP and Overdub without hits do not pass the lesson. | Android behavior test; LearnEngine tests |
| Timing Correct | Straight/triplet grid, Swing 50–75, tick shift, global on/off and DO IT for current-track event starts. Not Length/Legato/strength/window. | Existing pitch/TC regressions and code review |
| Tempo | Changing BPM preserves musical position. Count-in can be cancelled by STOP. | Playback math tests; Android cancellation/resume test |
| Note Repeat | Repeats held pad by selected TC interval; first tap alone no longer completes practice. Uses Android handler timing, not sample-accurate hardware scheduling. | Interval regressions and code review; latency needs device |
| Full Level | Velocity 127; ordinary phone taps already use a fixed level, so toggle may sound identical. Course explicitly explains why. | Gain tests; code review |
| 16 Levels | Velocity and Tune audition; original-pad reference in Tune. Tune recording is not implemented and is explicitly labelled in lessons. | Existing mode regressions; gain tests |
| Step Seq | Two bars, 16 fixed sixteenth-note steps per bar, selected pad and track. Does not pretend to implement all MPC step resolutions or note editing. | Code and lesson review |
| Mixer | Pad volume and master; velocity stored independently and played consistently. Volume-button changes report to training. | Gain tests; control wiring review |
| Pitch | Repitch ±12 semitones; changes duration with pitch. Persisted and resettable. | Existing pitch regressions |
| Sample Edit | PCM waveform, Start/End selection and even slices; validates remaining pad capacity and uses a unique output directory per chop. No transient slicing/warping. | WavSlicer and waveform tests; sample-kit chop test |
| Track Mute | Mutes sequenced events without deleting them; live pad audition remains possible. | Android behavior test |
| Save | One current project, assignments/events/levels/TC/pitch settings; not an audio export. | Persistence review and existing reset tests |
| Learning | 18 original Russian explanations with purpose, audible example, listening guidance, practice and a comprehension check. Free kit uses only empty pad slots. | Android lesson/quiz/kit tests and all-example WAV checks |

## Remaining scope and verification limits

- No physical phone/emulator audio latency or touch ergonomics validation is claimed by the JVM Android tests.
- SoundPool is intended for short one-shot samples; long-source pad playback needs a separate implementation and device verification. Browser preview uses streaming audio.
- Tune note recording, undo/redo and full MPC 3 track/arrangement behavior are not yet implemented.
- UI geometry is not certified by these behavior tests. Teaching text is scrollable in its own dialog, preserving the existing pad surface.
