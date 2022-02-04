@file:Verik

package camera

import io.verik.core.*
import imported.CameraBlkMem
import imported.VgaClkGen

@SynthTop
class FpgaTop(

    @In var clk_100mhz: Boolean,

    @In var ja: Ubit<`8`>,
    @In var jb: Ubit<`3`>,
    @Out var jbclk: Boolean,

    @In var jc: Ubit<`8`>,
    @In var jd: Ubit<`3`>,
    @Out var jdclk: Boolean,

    @Out var vga_r: Ubit<`4`>,
    @Out var vga_b: Ubit<`4`>,
    @Out var vga_g: Ubit<`4`>,
    @Out var vga_hs: Boolean,
    @Out var vga_vs: Boolean,

    @In var sw: Ubit<`16`>,
    @In var btnd: Boolean,

    @Out var led: Ubit<`16`>,

    @Out var ca: Boolean,
    @Out var cb: Boolean,
    @Out var cc: Boolean,
    @Out var cd: Boolean,
    @Out var ce: Boolean,
    @Out var cf: Boolean,
    @Out var cg: Boolean,
    @Out var dp: Boolean,
    @Out var an: Ubit<`8`>

) : Module() {

    var clk_65mhz: Boolean = nc()

    @Make
    val clk_gen = VgaClkGen(
        clk_out1 = clk_65mhz,
        reset = false,
        locked = nc(),
        clk_in1 = clk_100mhz
    )

    var hcount: Ubit<`11`> = nc()
    var vcount: Ubit<`10`> = nc()
    var hsync: Boolean = nc()
    var vsync: Boolean = nc()
    var blank: Boolean = nc()

    @Make
    val vga_signal_gen = VgaSignalGen(
        vclock_in = clk_65mhz,
        hcount_out = hcount,
        vcount_out = vcount,
        hsync_out = hsync,
        vsync_out = vsync,
        blank_out = blank
    )

    var pclk_buff: Boolean = nc()
    var pclk_in: Boolean = nc()
    var vsync_buff: Boolean = nc()
    var vsync_in: Boolean = nc()
    var href_buff: Boolean = nc()
    var href_in: Boolean = nc()
    var pixel_buff: Ubit<`8`> = nc()
    var pixel_in: Ubit<`8`> = nc()

    var pclk_buff_2: Boolean = nc()
    var pclk_in_2: Boolean = nc()
    var vsync_buff_2: Boolean = nc()
    var vsync_in_2: Boolean = nc()
    var href_buff_2: Boolean = nc()
    var href_in_2: Boolean = nc()
    var pixel_buff_2: Ubit<`8`> = nc()
    var pixel_in_2: Ubit<`8`> = nc()

    var xclk_count: Ubit<`2`> = nc()

    @Seq
    fun seqBufferInput() {
        on(posedge(clk_65mhz)) {
            pclk_buff = jb[0]
            vsync_buff = jb[1]
            href_buff = jb[2]
            pixel_buff = ja

            pclk_in = pclk_buff
            vsync_in = vsync_buff
            href_in = href_buff
            pixel_in = pixel_buff

            pclk_buff_2 = jd[0]
            vsync_buff_2 = jd[1]
            href_buff_2 = jd[2]
            pixel_buff_2 = jc

            pclk_in_2 = pclk_buff_2
            vsync_in_2 = vsync_buff_2
            href_in_2 = href_buff_2
            pixel_in_2 = pixel_buff_2

            xclk_count += u(1)
        }
    }

    @Com
    fun comJbclk() {
        jbclk = xclk_count > u(0b01)
        jdclk = xclk_count > u(0b01)
    }

    var displayMode: Ubit<`2`> = nc()
    var multiplier: Int = 6

    @Seq
    fun changeDisplayMode() {

        on(posedge(clk_65mhz)) {

            multiplier--

            if (multiplier == 0) {
                displayMode += if (btnd) u("2'b1") else u0()
                multiplier = 6
            }

        }

    }

    var output_pixels: Ubit<`16`> = nc()
    var valid_pixel: Boolean = nc()
    var frame_done_out: Boolean = nc()

    var output_pixels_2: Ubit<`16`> = nc()
    var valid_pixel_2: Boolean = nc()
    var frame_done_out_2: Boolean = nc()

    @Make
    val camera_read = CameraRead(
        p_clk_in = pclk_in,
        vsync_in = vsync_in,
        href_in = href_in,
        p_data_in = pixel_in,
        pixel_data_out = output_pixels,
        pixel_valid_out = valid_pixel,
        frame_done_out = frame_done_out
    )

    @Make
    val camera_read_2 = CameraRead(
        p_clk_in = pclk_in_2,
        vsync_in = vsync_in_2,
        href_in = href_in_2,
        p_data_in = pixel_in_2,
        pixel_data_out = output_pixels_2,
        pixel_valid_out = valid_pixel_2,
        frame_done_out = frame_done_out_2
    )

    var pixel_addr_in: Ubit<`17`> = nc()
    var pixel_addr_in_2: Ubit<`17`> = nc()

    var COM_x_1 : Ubit<`33`> = nc() // TODO: do we really need this many bits...
    var COM_y_1 : Ubit<`33`> = nc()
    var COM_N_1 : Ubit<`22`> = nc()

    var COM_x_2 : Ubit<`33`> = nc() // TODO: do we really need this many bits...
    var COM_y_2 : Ubit<`33`> = nc()
    var COM_N_2 : Ubit<`22`> = nc()

    var COM_out_x_1 : Ubit<`11`> = nc()
    var COM_out_y_1 : Ubit<`10`> = nc()

    var COM_out_x_2 : Ubit<`11`> = nc()
    var COM_out_y_2 : Ubit<`10`> = nc()

    var test : Ubit<`12`> = nc()
    var test2 : Ubit<`12`> = nc()

    @Seq
    fun seqPixelAddr() {
        on(posedge(pclk_in)) {
            pixel_addr_in = if (frame_done_out) u0() else pixel_addr_in + u(1)
            pixel_addr_in = when {
                frame_done_out -> u0()
                valid_pixel -> pixel_addr_in + u(1)
                else -> pixel_addr_in
            }
        }
    }

    @Seq
    fun seqPixelAddr2() {
        on(posedge(pclk_in_2)) {
            pixel_addr_in_2 = if (frame_done_out_2) u0() else pixel_addr_in_2 + u(1)
            pixel_addr_in_2 = when {
                frame_done_out_2 -> u0()
                valid_pixel_2 -> pixel_addr_in_2 + u(1)
                else -> pixel_addr_in_2
            }
        }
    }

    var upscale: Int = 0

    @Com
    var pixel_addr_out: Ubit<`17`> = (hcount shr upscale) + ((vcount shr upscale) mul u(320)).tru<`17`>()

    @Com
    var pixel_addr_out_2: Ubit<`17`> = pixel_addr_out // TODO: same as _1

//    var count: Ubit<`28`> = nc()
//
//    @Seq
//    fun setCount() {
//        on(posedge(clk_100mhz)) {
//            if (count == u1<`28`>()) count = u0<`28`>()
//            else count += u(1)
//        }
//    }

    @Com
    var processed_pixels: Ubit<`12`> = cat(
            output_pixels.sli<`4`>(12),
            output_pixels.sli<`4`>(7),
            output_pixels.sli<`4`>(1)
        )

    @Com
    var processed_pixels_2: Ubit<`12`> = cat(
        output_pixels_2.sli<`4`>(12),
        output_pixels_2.sli<`4`>(7),
        output_pixels_2.sli<`4`>(1)
    )

    var frame_buff_out: Ubit<`12`> = nc()
    var frame_buff_out_2: Ubit<`12`> = nc()

    @Make
    val blk_mem = CameraBlkMem(
        addra = pixel_addr_in,
        clka = pclk_in,
        dina = processed_pixels,
        wea = valid_pixel.toUbit(),
        addrb = pixel_addr_out,
        clkb = clk_65mhz,
        doutb = frame_buff_out
    )

    @Make
    val blk_mem_2 = CameraBlkMem(
        addra = pixel_addr_in_2,
        clka = pclk_in_2,
        dina = processed_pixels_2,
        wea = valid_pixel_2.toUbit(),
        addrb = pixel_addr_out_2,
        clkb = clk_65mhz,
        doutb = frame_buff_out_2
    )

    var rgb_out: Ubit<`12`> = nc()
    var blank_out: Boolean = nc()
    var hsync_out: Boolean = nc()
    var vsync_out: Boolean = nc()

    var hue1: Ubit<`6`> = nc()
    var hue2: Ubit<`6`> = nc()

    var hueMask1: Ubit<`12`> = nc()
    var hueMask2: Ubit<`12`> = nc()

    var CNSTCOLOR: Ubit<`4`> = u("4'b0011")

    @Seq
    fun processImage() {

        on(posedge(pclk_in)) {

            var R: Ubit<`4`> = frame_buff_out.sli<`4`>(0)
            var G: Ubit<`4`> = frame_buff_out.sli<`4`>(4)
            var B: Ubit<`4`> = frame_buff_out.sli<`4`>(8)

            // convert to hue
            if ( (R > G) and (R > B) ) { // red max

                if ((R-G < CNSTCOLOR) and (R-B < CNSTCOLOR)) {

                    hue1 = u0()
                    hueMask1 = u("12'b000000000000")

                } else {

                    hue1 = cat(u("2'b01"), R - max(G, B)) //G-B)
                    hueMask1 = cat(u("4'b0000"), u("4'b0000"), R - max(G, B)) //frame_buff_out and u("12'b000000001111")

                }

            } else if ( (B > G) and (B > R) ) { // blue max

                if ((B-G < CNSTCOLOR) and (B-R < CNSTCOLOR)) {

                    hue1 = u0()
                    hueMask1 = u("12'b000000000000")

                } else {

                    hue1 = cat(u("2'b10"), B - max(G, R))//R-G)
                    hueMask1 = cat(B - max(G, R), u("4'b0000"), u("4'b0000")) //frame_buff_out and u("12'b111100000000")

                }

            } else if ( (G > B) and (G > R) ) { // green max

                if ((G-B < CNSTCOLOR) and (G-R < CNSTCOLOR)) {

                    hue1 = u0()
                    hueMask1 = u("12'b000000000000")

                } else {

                    hue1 = cat(u("2'b11"), G - max(B, R))//B-R)
                    hueMask1 = cat(u("4'b0000"), G - max(B, R), u("4'b0000"))//frame_buff_out and u("12'b000011110000")

                }

            } else {

                hue1 = u0()
                hueMask1 = u("12'b000000000000")

            }

            if (frame_done_out) {

                COM_out_x_1 = (COM_x_1 / COM_N_1).tru()
                COM_out_y_1 = (COM_y_1 / COM_N_1).tru()

                COM_x_1 = u0()
                COM_y_1 = u0()
                COM_N_1 = u0()

                test = u0()

            } else {
                // get upper three bits of val, and if there sum is greater than const then is part of OBJ

                if ( hcount > u(20) && vcount > u(20) && hcount < u(300) && vcount < u(220) ) { // TODO: revisit if condition can help



//                    var pixel_mag : Ubit<`6`> = (if (sw[14]) frame_buff_out.sli<`4`>(0) else u0<`4`>()) add
//                            (if (sw[13]) frame_buff_out.sli<`4`>(4) else u0<`4`>()) add
//                            (if (sw[12]) frame_buff_out.sli<`4`>(8) else u0<`4`>())

                    var looking_for_r: Boolean = sw[15]
                    var looking_for_g: Boolean = sw[14]
                    var looking_for_b: Boolean = sw[13]

                    var pixelHueMajor: Ubit<`2`> = hue1.sli<`2`>(4)

                    var pixel_mag : Ubit<`6`> = (if (looking_for_r && (pixelHueMajor==u("2'b00"))) hue1.sli<`4`>(0) else u0<`4`>()) add
                                                (if (looking_for_b && (pixelHueMajor==u("2'b01"))) hue1.sli<`4`>(0) else u0<`4`>()) add
                                                (if (looking_for_g && (pixelHueMajor==u("2'b11"))) hue1.sli<`4`>(0) else u0<`4`>())

                    test = u0() //if (pixel_mag > u("5'b01100")) u("12'b1111_0000_1111") else u0()
                    if (pixel_mag > sw.sli<`6`>(0)) {

                        COM_x_1 += hcount
                        COM_y_1 += vcount
                        COM_N_1 += u("21'b1")

                        if (displayMode == u("2'd3")) {

                            if (test > u("12'b1000_0000_1000")) {
                                test = u("12'b1111_0000_1111")
                            } else {
                                test = u("12'b0111_0000_0111") + test
                            }

                        }

                    }
                } else {

                    test = u0()

                }
            }

        }

    }

    @Seq
    fun processImage2() {

        on(posedge(pclk_in_2)) {

            var R: Ubit<`4`> = frame_buff_out_2.sli<`4`>(0)
            var G: Ubit<`4`> = frame_buff_out_2.sli<`4`>(4)
            var B: Ubit<`4`> = frame_buff_out_2.sli<`4`>(8)
            // convert to hue
            if ( (R > G) and (R > B) ) { // red max

                if ((R-G < CNSTCOLOR) and (R-B < CNSTCOLOR)) {

                    hue2 = u0()
                    hueMask2 = u("12'b000000000000")

                } else {

                    hue2 = cat(u("2'b01"), G-B)
                    hueMask2 = frame_buff_out_2 and u("12'b000000001111")

                }

            } else if ( (B > G) and (B > R) ) { // blue max

                if ((B-G < CNSTCOLOR) and (B-R < CNSTCOLOR)) {

                    hue2 = u0()
                    hueMask2 = u("12'b000000000000")

                } else {

                    hue2 = cat(u("2'b10"), R-G)
                    hueMask2 = frame_buff_out_2 and u("12'b111100000000")

                }

            } else { // green max

                if ((G-B < CNSTCOLOR) and (G-R < CNSTCOLOR)) {

                    hue2 = u0()
                    hueMask2 = u("12'b000000000000")

                } else {

                    hue2 = cat(u("2'b11"), B-R)
                    hueMask2 = frame_buff_out_2 and u("12'b000011110000")

                }

            }

            if (frame_done_out_2) {

                COM_out_x_2 = (COM_x_2 / COM_N_2).tru()
                COM_out_y_2 = (COM_y_2 / COM_N_2).tru()

                COM_x_2 = u0()
                COM_y_2 = u0()
                COM_N_2 = u0()

                test2 = u0()

            } else {
                // get upper three bits of val, and if there sum is greater than const then is part of OBJ

                if ( hcount > u(360) && vcount > u(20) && hcount < u(600) && vcount < u(220) ) { // TODO: revisit if condition can help
//                    var pixel_mag : Ubit<`6`> = (if (sw[14]) frame_buff_out_2.sli<`4`>(0) else u0<`4`>()) add
//                                                (if (sw[13]) frame_buff_out_2.sli<`4`>(4) else u0<`4`>()) add
//                                                (if (sw[12]) frame_buff_out_2.sli<`4`>(8) else u0<`4`>())



                var looking_for_r: Boolean = sw[15]
                var looking_for_g: Boolean = sw[14]
                var looking_for_b: Boolean = sw[13]

                var pixelHueMajor: Ubit<`2`> = hue2.sli<`2`>(4)

                var pixel_mag : Ubit<`6`> = (if (looking_for_r && (pixelHueMajor==u("2'b00"))) hue2.sli<`4`>(0) else u0<`4`>()) add
                                            (if (looking_for_b && (pixelHueMajor==u("2'b01"))) hue2.sli<`4`>(0) else u0<`4`>()) add
                                            (if (looking_for_g && (pixelHueMajor==u("2'b11"))) hue2.sli<`4`>(0) else u0<`4`>())

                    test2 = u0() //if (pixel_mag > u("5'b01100")) u("12'b1111_0000_1111") else u0()
                    if (pixel_mag > sw.sli<`6`>(6)) {

                        COM_x_2 += hcount
                        COM_y_2 += vcount
                        COM_N_2 += u("21'b1")

                        if (displayMode == u("2'd3")) {

                            if (test2 > u("12'b1000_0000_1000")) {
                                test2 = u("12'b1111_0000_1111")
                            } else {
                                test2 = u("12'b0111_0000_0111") + test2
                            }

                        }

                    }
                } else {

                    test2 = u0()

                }
            }

        }

    }

    @Seq
    fun distance_processing() {

        on(posedge(clk_65mhz)) {

            var pix_dist = COM_out_x_2 - COM_out_x_1 - u("11'd320")
            led = ( pix_dist shl 5 ).ext() // shifting 5 because led[4] doesn't work :(

        }

    }

    var X1_in: Ubit<`9`> = nc()
    var X2_in: Ubit<`9`> = nc()

    @Seq
    fun seqOutput() {
        on(posedge(clk_65mhz)) {

//            led[0] = COM_out_x_1[0]
//            led[1] = COM_out_x_1[1]
//            led[2] = COM_out_x_1[2]
//            led[3] = COM_out_x_1[3]
//            led[4] = COM_out_x_1[4] // led[4] doesn't work on the board
//            led[5] = COM_out_x_1[5]
//            led[6] = COM_out_x_1[6]
//            led[7] = COM_out_x_1[7]
//
//
//            led[8]  = COM_out_x_2[0]
//            led[9]  = COM_out_x_2[1]
//            led[10] = COM_out_x_2[2]
//            led[11] = COM_out_x_2[3]
//            led[12] = COM_out_x_2[4]
//            led[13] = COM_out_x_2[5]
//            led[14] = COM_out_x_2[6]
//            led[15] = COM_out_x_2[7]

            hsync_out = hsync
            vsync_out = vsync
            blank_out = blank

            var modifier_2_x : Ubit<`11`> = u0()
            var modifier_2_y : Ubit<`10`> = u0()

            if (displayMode.sli<`1`>(0) == u("1'b1")) {

                modifier_2_x = u("11'd320") // TODO: make this into a global lumped w/ val in dist_proc
                modifier_2_y = u0()

            }

            var disp1 : Ubit<`12`> = frame_buff_out
            var disp2 : Ubit<`12`> = frame_buff_out_2

            if (sw[12]) {
                disp1 = hueMask1
                disp2 = hueMask2
            }

            var rgb_out_val : Ubit<`12`>  = if (hcount < u(320) && vcount < u(240))
                                                disp1 else test+test2
            rgb_out_val = if (hcount < u(320) && vcount < u(240) && test > u0<`12`>())
                                                test else rgb_out_val
            rgb_out_val = if (hcount > u(320) && hcount < u(640) && vcount < u(240))
                                                disp2 else rgb_out_val
            rgb_out_val = if (hcount > u(320) && hcount < u(640) && vcount < u(240) && test2 > u0<`12`>())
                                                test2 else rgb_out_val

            rgb_out_val  = if (hcount <= COM_out_x_1 + u(1) && hcount >= COM_out_x_1 - u(1)) u("12'b1111_0000_0000") else rgb_out_val
            rgb_out_val  = if (vcount <= COM_out_y_1 + u(1) && vcount >= COM_out_y_1 - u(1)) u("12'b0000_0000_1111") else rgb_out_val

            rgb_out_val  = if (hcount <= COM_out_x_2 - modifier_2_x + u(1) && hcount >= COM_out_x_2 - modifier_2_x - u(1)) u("12'b1111_0000_1111") else rgb_out_val
            rgb_out_val  = if (vcount <= COM_out_y_2 - modifier_2_y + u(1) && vcount >= COM_out_y_2 - modifier_2_y - u(1)) u("12'b0000_1111_1111") else rgb_out_val

            rgb_out = rgb_out_val

            X1_in = COM_out_x_1.sli<`9`>(2)
            X2_in = ( COM_out_x_2 - u("11'd320") ).sli<`9`>(2)


        }
    }

    var alldistdata : Ubit<`32`> = nc()

    @Make
    val dist_lut = distanceLUT(
        clk_in = clk_65mhz,
        X1 = X1_in,
        X2 = X2_in,
        outputdistance = alldistdata
    )

    var seg: Ubit<`7`> = nc()

    var seg_counter = 0
    var active_led: Ubit<`3`> = u0()

    var displayDist: Ubit<`4`> = nc()


    @Make
    val seven_segment = SevenSegment(
        digit = displayDist.ext(),
        seg = seg)

    @Com
    fun set_output() {
        ca = seg[0]
        cb = seg[1]
        cc = seg[2]
        cd = seg[3]
        ce = seg[4]
        cf = seg[5]
        cg = seg[6]
    }

    @Seq
    fun seg_display_drive() {
        on(posedge(clk_100mhz)) {
            seg_counter++
            if (seg_counter > 9999) {
                seg_counter = 0

                when (active_led) {
                    u("3'b000") -> {
                        an = u("8'b11111110")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(0)
                    }
                    u("3'b001") -> {
                        an = u("8'b11111101")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(4)
                    }
                    u("3'b010") -> {
                        an = u("8'b11111011")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(8)
                    }
                    u("3'b011") -> {
                        an = u("8'b11110111")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(12)
                    }
                    u("3'b100") -> {
                        an = u("8'b11101111")
                        dp = false // turn dp at this place
                        displayDist = alldistdata.sli<`4`>(16)
                    }
                    u("3'b101") -> {
                        an = u("8'b11011111")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(20)
                    }
                    u("3'b110") -> {
                        an = u("8'b10111111")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(24)
                    }
                    else -> {
                        an = u("8'b01111111")
                        dp = true
                        displayDist = alldistdata.sli<`4`>(28)
                    }
                }

                active_led++
            }
        }
    }

    @Com
    fun comOutput() {
        vga_r = if (!blank_out) rgb_out.sli(8) else u0()
        vga_g = if (!blank_out) rgb_out.sli(4) else u0()
        vga_b = if (!blank_out) rgb_out.sli(0) else u0()
        vga_hs = !hsync_out
        vga_vs = !vsync_out
    }
}
