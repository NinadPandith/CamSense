# Content Guidelines: CamSense

Oct 1, 2026 · @Ninad Pandith M N

How CamSense looks, sounds and writes, using the Charcoal lime palette. Companion to Doc and Doc.

## Brand essence

CamSense should feel like a precise pocket detector in the hands of a calm expert: bold enough to trust, never alarming.

**Promise:** "Check any room for hidden cameras in five minutes, using just your phone."

**Personality**

| We are                      | We are not                        |
|-----------------------------|-----------------------------------|
| Calm and steady             | Alarmist or dramatic              |
| Clear and direct            | Technical or full of jargon       |
| Precise, like an instrument | Gimmicky or "hacker" styled       |
| Honest about limits         | Promising 100% detection          |
| On the user's side          | Judgemental about where they stay |

**The look in one line:** a dark graphite instrument panel with lime signals, where lime means "act here" and red, amber and green mean risk.

## Colour palette: Charcoal lime

The app is dark by default so screens don't light up the room during a scan. Lime is reserved for actions; risk always uses red, amber or green with an icon and a word.

**Core colours**

| Token          | Hex             | Use                                        |
|----------------|-----------------|--------------------------------------------|
| background     | \#111311        | Screen background                          |
| surface        | \#1D211D        | Cards, sheets, bottom bars                 |
| surface-raised | \#272C27        | Pressed cards, dialogs                     |
| border         | \#333933        | Dividers, card outlines                    |
| primary (lime) | \#A3E635        | Main buttons, active step, links, progress |
| on-primary     | \#1A2605        | Text and icons on lime                     |
| primary-muted  | \#A3E635 at 15% | Selected chips, focus halo                 |
| text           | \#F1F5EE        | Headings and body text                     |
| text-muted     | \#94A08F        | Helper text, timestamps, captions          |

**Risk colours** (used only for risk, never for decoration)

| Level  | Fill / icon | Text on dark | Tint background | Icon              |
|--------|-------------|--------------|-----------------|-------------------|
| Low    | \#22C55E    | \#4ADE80     | \#22C55E at 15% | Shield with check |
| Medium | \#F59E0B    | \#FBBF24     | \#F59E0B at 15% | Eye               |
| High   | \#EF4444    | \#F87171     | \#EF4444 at 15% | Alert triangle    |

**Camera overlay colours** (drawn on the live preview)

| Overlay                | Colour   | Style                            |
|------------------------|----------|----------------------------------|
| Suspected lens glint   | \#EF4444 | 2 dp circle with pulsing ring    |
| IR glow                | \#F59E0B | 2 dp circle                      |
| AI "check this" object | \#A3E635 | 2 dp rounded box with label chip |

**Rules**

- Lime and Low-risk green are both green-yellow hues. Never place them side by side, and always show the risk word and icon so the two are never confused.

- One lime button per screen. Secondary actions use a lime outline or lime text.

- No gradients, glows or neon effects; flat colours only.

- An optional light theme (for daytime use) can come later: background \#F5F7F2, surface \#FFFFFF, primary \#4D7C0F, text \#111311.

## Typography

Use Space Grotesk for headings (the instrument feel), Inter for everything people read, and JetBrains Mono for technical values. All three are free Google Fonts (SIL Open Font License); bundle them in res/font so the app stays offline.

| Style | Font | Size / line height | Weight | Use |
|----|----|----|----|----|
| Display | Space Grotesk | 32 / 40 sp | Bold | Risk result ("High risk") |
| Title | Space Grotesk | 22 / 28 sp | Semibold | Screen titles |
| Heading | Space Grotesk | 18 / 24 sp | Semibold | Card titles, step names |
| Body | Inter | 16 / 24 sp | Regular | Instructions, explanations |
| Label | Inter | 14 / 20 sp | Medium | Buttons, chips, tabs |
| Caption | Inter | 12 / 16 sp | Regular | Timestamps, helper text |
| Data | JetBrains Mono | 14 / 20 sp | Regular | IP addresses, ports, signal values |

**Rules**

- Sentence case everywhere: "Start scan", not "Start Scan" or "START SCAN".

- Never go below 12 sp; people read this in the dark, often at arm's length.

- Respect the phone's font-size setting (use sp, not dp, for text).

## Icons and imagery

Use Material Symbols Rounded (outlined, weight 400) for every icon, so the set stays consistent and ships with Compose.

| Meaning         | Icon                |
|-----------------|---------------------|
| M1 Lens glint   | flare               |
| M2 Infrared     | wb_iridescent       |
| M3 Wi-Fi scan   | router              |
| M4 Bluetooth    | bluetooth_searching |
| M5 Magnetometer | explore             |
| M6 AI objects   | center_focus_strong |
| Low risk        | verified_user       |
| Medium risk     | visibility          |
| High risk       | warning             |
| Evidence        | photo_camera        |
| Scan history    | history             |

**Rules**

- Icons are 24 dp in lists and buttons, 48 dp on result and step screens.

- Icons take the text colour unless they show risk or an active state.

- Step illustrations are simple flat line drawings of the phone and room in lime and grey; no photos of people, no scary imagery (eyes, shadowy figures, hidden watchers).

- App icon: a lime rounded lens ring with a small scan line on the graphite background.

## Layout, spacing and components

Everything sits on an 8 dp grid with big touch targets, because users hold the phone in one hand in a dark room.

| Element | Spec |
|----|----|
| Spacing scale | 4, 8, 12, 16, 24, 32, 48 dp |
| Screen side padding | 20 dp |
| Corner radius | 12 dp cards, 10 dp buttons, 8 dp chips |
| Touch target | 48 dp minimum; main scan buttons 56 dp tall, full width |
| Primary button | Lime fill, on-primary text, bottom of screen within thumb reach |
| Secondary button | 1.5 dp lime outline, lime text |
| Card | Surface fill, 1 dp border, 16 dp padding |
| Risk chip | Risk tint background, risk text colour, icon + word |
| Step progress | Six dots at the top; done = lime, current = lime ring, next = border colour |
| Camera screens | Full-bleed preview; one instruction line in a surface pill at the top; one button at the bottom |

**Screen rules**

- One instruction and one main action per screen.

- During the dark sweep, dim the UI to 70% brightness and hide everything except the preview, the overlays and the stop button.

- Every alert on the results screen opens a detail card with the photo, the module name and the reason.

## Voice and tone

CamSense speaks like a calm, knowledgeable friend doing a safety check with you: plain words, short sentences, no panic.

| Principle | What it means | Example |
|----|----|----|
| Calm | State findings without drama, even for High risk | "Strong signs of a hidden camera" not "DANGER! You're being watched!" |
| Clear | Everyday words; explain any technical term the first time | "A device on this Wi-Fi is streaming video" not "RTSP open on port 554" |
| Honest | Show uncertainty and limits; never guarantee | "No clear signs found" not "This room is safe" |
| Helpful | Every finding says what to do next | "Check the tissue box on the shelf" |
| Respectful | Never blame the user or shame a place | "Report it to the hotel" not "You picked a bad hotel" |

**Tone by moment**

| Moment                    | Tone                          |
|---------------------------|-------------------------------|
| Onboarding and scan steps | Friendly, encouraging, short  |
| Low result                | Reassuring but honest         |
| Medium result             | Focused and practical         |
| High result               | Serious, steady, action-first |
| Errors                    | Matter-of-fact, with a fix    |

## Writing mechanics

- **Sentence case** for everything: buttons, titles, chips, menu items.

- **Buttons are verbs**, 1–3 words: "Start scan", "Save evidence", "Check again". Never "OK" or "Submit".

- **Instructions:** one sentence, under 12 words, starting with a verb: "Turn off the room lights."

- **Explanations:** at most 2 short sentences on a screen.

- **Address the user as "you"**; the app never says "I" or "we" inside the scan.

- **Contractions are fine:** "you'll", "don't", "it's".

- **No exclamation marks, no ALL CAPS, no "please", no "successfully".**

- **Numbers:** digits for all numbers ("3 devices", "2 m away"); metric units.

- **Technical values** (IPs, ports) only in detail cards, in the Data style, always after a plain-language line.

- **Words to use / avoid**

| Use                                | Avoid                   |
|------------------------------------|-------------------------|
| Possible camera, signs of a camera | Spy, creep, predator    |
| Check, scan, look closer           | Hack, attack, sniff     |
| No clear signs found               | Safe, clean, 100% clear |
| Save evidence                      | Capture proof, gotcha   |

## Risk result messaging

Every result screen has the same four parts: level, one-line summary, reasons, and next steps. High never says "you are being recorded"; Low never says "safe".

| Part | Low | Medium | High |
|----|----|----|----|
| Level | Low risk | Medium risk | High risk |
| Summary | No clear signs of a camera found. | Some signs found. Check the spots below. | Strong signs of a hidden camera. |
| Reason example | All 6 checks ran; nothing stood out. | A bright reflection near the clock. | Lens reflection in the tissue box, and a device on this Wi-Fi is streaming video. |
| Next steps | Stay alert. Some cameras can't be found by any phone. | Look closely at each flagged spot, then scan again. | Don't touch the object. Save evidence, leave the room, and report it to the manager or police. |
| Main button | Done | Check flagged spots | Save evidence |
| Second action | Scan another room | Scan again | Get help |

**Always add this line under every result, in caption style:** "CamSense raises the chance of finding a camera but can't guarantee it."

**Get help** opens a screen with the national emergency number 112, the cybercrime helpline 1930, and the cybercrime.gov.in reporting site, plus a tip to keep the saved evidence. Verify these numbers again before release.

## In-app copy library

Use these strings as written; put them in strings.xml so they can be translated later.

**Guided scan**

| Screen | Title | Instruction | Button |
|----|----|----|----|
| Welcome | Check this room | Takes about 5 minutes. Everything stays on your phone. | Start scan |
| 1\. Place name | Where are you? | Add a name so you can find this scan later. | Next |
| 2\. Network check | Checking nearby devices | Looking at this Wi-Fi and Bluetooth. Keep going while it runs. | Continue |
| 3\. Room sweep | Sweep the room | Move slowly around the room. CamSense marks places cameras often hide. | Done sweeping |
| 4a. IR check | Turn off the lights | Point your screen side around the room. Look for glowing dots. | Next |
| 4b. Lens check | Look for reflections | Flashlight is on. Scan slowly; watch for tiny bright points. | Done |
| 5\. Close check | Check flagged spots | Hold your phone close to each one. Faster beeps mean more electronics. | See results |

**Permission explanations** (shown before the system prompt)

| Permission | Copy |
|----|----|
| Camera | Your camera finds lens reflections and night-vision lights. Images never leave your phone. |
| Nearby devices | This lets CamSense check the Wi-Fi and Bluetooth around you for camera-like devices. |

**Errors and empty states**

| Situation | Copy |
|----|----|
| Not on Wi-Fi | You're not on Wi-Fi, so the network check was skipped. Join the room's Wi-Fi to include it. |
| Front camera can't see IR | Your phone's front camera can't see infrared, so this check was skipped. Other checks still work. |
| Permission denied | Without camera access, the lens and IR checks can't run. Allow it in Settings. |
| Room too bright | It's too bright for this check. Turn off the lights and close the curtains. |
| Moving too fast | Slow down a little so the camera can keep up. |
| No history yet | Your scans will show up here. Start your first scan. |

## Accessibility

- **Contrast:** body text at least 4.5:1 and large text 3:1 against its background. The palette meets this: text \#F1F5EE and lime \#A3E635 on \#111311 are both well above 10:1. Use the lighter risk text colours (#4ADE80, \#FBBF24, \#F87171) for any risk words on dark surfaces.

- **Never colour alone:** every risk and every overlay also has an icon and a word.

- **Sound and vibration:** the magnetometer beep also vibrates, and a High result plays one short vibration, so the scan works without looking or hearing.

- **TalkBack:** every icon button has a content description; overlays announce "Possible lens found, top left."

- **Text size:** layouts must still work at 200% system font size.

- **Motion:** pulsing overlay rings stop when the phone's "Remove animations" setting is on.

## Do and don't

| Do | Don't |
|----|----|
| "Strong signs of a hidden camera." | "WARNING!!! Spy camera detected!" |
| "No clear signs of a camera found." | "This room is 100% safe." |
| "A device on this Wi-Fi is streaming video." | "RTSP stream detected on 192.168.1.14:554." |
| "Check the tissue box on the shelf." | "Suspicious object \#3 flagged by M6." |
| One lime button at the bottom of the screen | Two lime buttons competing on one screen |
| Risk shown with colour, icon and word | Risk shown by colour alone |
| Flat graphite surfaces with lime accents | Neon glows, gradients or "hacker" green text on black |
| Calm line illustrations | Scary images of eyes or hidden watchers |
