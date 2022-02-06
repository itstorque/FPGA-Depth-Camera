# FPGA Depth Estimation using a Camera Array 

This is a project built using the [Verik toolchain](https://verik.io/), synthesized using Vivado and uploaded onto a Nexys 4 
Artix-7 FPGA Board. It uses the feed from two cameras and uses color segmentation to estimate the distance it is away from an 
object in meters. The cameras registers are initialized using a Lolin D1 Mini Pro and have the data streamed without modification
onto the FPGA, where all the processing happens.

There are multiple debug modes on the FPGA. The main mode is can be toggled using the 13th switch (`sw[12]`) and it toggles
the color segmentation mode. The other modes can be flipped through using the down button on the FPGA, it toggles pixel selection
and overlays the crosshairs over each other for easier visibility during debugging.

When running, the FPGA outputs two image streams using its VGA board with a crosshair on each centered on a color blob whenever 
it is using the color segmentation algorithm. The last three switches (`sw[13:15]`) toggle between segmenting over the R, G and B 
respectively. The rest of the switches (`sw[0:5]` and `sw[6:11]`) help fine tune the segmentation and calibrate over different lighting
conditions where each set controls the left and right output image respectively.

Color segmentation usually uses hue instead of rgb. In this case I used something analogous to hue that is easier to implement
on hardware that I defined as follows: keep track of the dominant channel, the value of that channel is how much more dominant it is
over the average of the other two. For displaying purposes, the other channels are zeroed out. For example, if a pixel has values
`0x45ef67` (R: `69`, G: `239`, B: `103`), the dominant channel is G and its value is `239 - ( 69 + 103 )/2 = 0d153 = 0x99` and so our output pixel is
`0x009900`.

The file structure is as below

- `.idea`: IntelliJ IDEA project configuration directory.
- `src/`: Project source directory.
- `build`: Build output directory.
- `build.gradle.kts`: Gradle build file.
- `settings.gradle.kts`: Gradle settings file.
- `ip/`: Contains all the ip imported into the projected.
- `ov7670_control`: Arduino code that controls the two Lolins.
- `LUTgenerator.py`: generates the look-up table that converts pixel locations to distance estimations.
