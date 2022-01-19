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

    @Out var led: Ubit<`16`>,

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

    @Seq
    fun processImage() {

        on(posedge(pclk_in)) {

            if (frame_done_out) {

                COM_out_x_1 = (((COM_x_1 / COM_N_1) * sw.sli<`4`>(5)) ).tru()
                COM_out_y_1 = (COM_y_1 / COM_N_1).tru()

//                led = COM_out_x_1.ext()

                COM_x_1 = u0()
                COM_y_1 = u0()
                COM_N_1 = u0()

                test = u0()

            } else {
                // get upper three bits of val, and if there sum is greater than const then is part of OBJ

                if ( hcount > u(20) && vcount > u(20) && hcount < u(300) && vcount < u(220) ) { // TODO: revisit if condition can help
                    var pixel_mag : Ubit<`5`> = frame_buff_out.sli<`3`>(1) add frame_buff_out.sli<`3`>(5) add frame_buff_out.sli<`3`>(9)
                    test = u0() //if (pixel_mag > u("5'b01100")) u("12'b1111_0000_1111") else u0()
                    if (pixel_mag > sw.sli<`5`>(11)) {

                        COM_x_1 += hcount
                        COM_y_1 += vcount
                        COM_N_1 += sw.sli<`4`>(0)//u("21'b10")

                        if (sw[15]) {

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

            if (frame_done_out_2) {

                COM_out_x_2 = (((COM_x_2 / COM_N_2) * sw.sli<`4`>(5)) ).tru()
                COM_out_y_2 = (COM_y_2 / COM_N_2).tru()

                COM_x_2 = u0()
                COM_y_2 = u0()
                COM_N_2 = u0()

                test2 = u0()

            } else {
                // get upper three bits of val, and if there sum is greater than const then is part of OBJ

                if ( hcount > u(20) && vcount > u(20) && hcount < u(300) && vcount < u(220) ) { // TODO: revisit if condition can help
                    var pixel_mag : Ubit<`5`> = frame_buff_out.sli<`3`>(1) add frame_buff_out.sli<`3`>(5) add frame_buff_out.sli<`3`>(9)
                    test2 = u0() //if (pixel_mag > u("5'b01100")) u("12'b1111_0000_1111") else u0()
                    if (pixel_mag > sw.sli<`5`>(10)) {

                        COM_x_2 += hcount
                        COM_y_2 += vcount
                        COM_N_2 += sw.sli<`4`>(0)//u("21'b10")

                        if (sw[15]) {

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
    fun seqOutput() {
        on(posedge(clk_65mhz)) {

            led[0] = COM_out_x_1[0]
            led[1] = COM_out_x_1[1]
            led[2] = COM_out_x_1[2]
            led[3] = COM_out_x_1[3]
            led[4] = COM_out_x_1[4] // led[4] doesn't work on the board
            led[5] = COM_out_x_1[5]
            led[6] = COM_out_x_1[6]
            led[7] = COM_out_x_1[7]


            led[8]  = COM_out_x_2[0]
            led[9]  = COM_out_x_2[1]
            led[10] = COM_out_x_2[2]
            led[11] = COM_out_x_2[3]
            led[12] = COM_out_x_2[4]
            led[13] = COM_out_x_2[5]
            led[14] = COM_out_x_2[6]
            led[15] = COM_out_x_2[7]


            hsync_out = hsync
            vsync_out = vsync
            blank_out = blank

            var rgb_out_val : Ubit<`12`>  = if (hcount < u(320) && vcount < u(240)) frame_buff_out+test else test+test2 //u0()
//            rgb_out_val  = if (hcount < u(320) && vcount < u(240) && test > u("12'b1")) test else rgb_out_val //  && vcount <= u(121) && vcount >= u(199)
            rgb_out_val = if (hcount > u(320) && hcount < u(640) && vcount < u(240)) frame_buff_out_2+test2 else rgb_out_val

            rgb_out_val  = if (hcount <= COM_out_x_1 + u(1) && hcount >= COM_out_x_1 - u(1)) u("12'b1111_0000_0000") else rgb_out_val
            rgb_out_val  = if (vcount <= COM_out_y_1 + u(1) && vcount >= COM_out_y_1 - u(1)) u("12'b0000_0000_1111") else rgb_out_val

            rgb_out_val  = if (hcount <= COM_out_x_2 + u(1) && hcount >= COM_out_x_2 - u(1)) u("12'b1111_0000_1111") else rgb_out_val
            rgb_out_val  = if (vcount <= COM_out_y_2 + u(1) && vcount >= COM_out_y_2 - u(1)) u("12'b0000_1111_1111") else rgb_out_val

            rgb_out = rgb_out_val
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
