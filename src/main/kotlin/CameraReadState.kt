@file:Verik

package camera

import io.verik.core.*

enum class CameraReadState {
    WAIT_FRAME_START,
    ROW_CAPTURE
}
