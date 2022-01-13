@file:Verik

import io.verik.core.*

@SynthTop
class FpgaTop(
    @In var clk_100mhz: Boolean,
    @In var sw: Ubit<`16`>,
    @Out var led: Ubit<`16`>
) : Module() {

    @Seq
    fun seqLed() {
        on (posedge(clk_100mhz)) {
            led = sw
        }
    }
}
