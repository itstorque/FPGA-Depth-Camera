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
    @Out var vga_vs: Boolean

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

    var COM_1 : Ubit<`17`> = nc()
    var COM_2 : Ubit<`17`> = nc()

    @Seq
    fun seqPixelAddr() {
        on(posedge(pclk_in)) {

            if (frame_done_out) {
                COM_1 = u0();
            } else {
                // get upper three bits of val, and if there sum is greater than const then is part of OBJ
                var pixel_mag : Ubit<`5`> = frame_buff_out.sli<`3`>(1) add frame_buff_out.sli<`3`>(5) add frame_buff_out.sli<`3`>(9)
                frame_buff_out = if (pixel_mag > u("5'b01100")) u("12'b1111_0000_0000") else frame_buff_out
            }

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
    fun seqOutput() {
        on(posedge(clk_65mhz)) {
            hsync_out = hsync
            vsync_out = vsync
            blank_out = blank

            var rgb_out_val : Ubit<`12`>  = if (hcount < u(320) && vcount < u(240)) frame_buff_out else u0()
            rgb_out_val = if (hcount > u(320) && hcount < u(640) && vcount < u(240)) frame_buff_out_2 else rgb_out_val

            rgb_out = rgb_out_val
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
    fun comOutput() {
        vga_r = if (!blank_out) rgb_out.sli(8) else u0()
        vga_g = if (!blank_out) rgb_out.sli(4) else u0()
        vga_b = if (!blank_out) rgb_out.sli(0) else u0()
        vga_hs = !hsync_out
        vga_vs = !vsync_out
    }
}
