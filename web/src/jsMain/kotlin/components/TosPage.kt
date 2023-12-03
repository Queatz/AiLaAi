package components
import androidx.compose.runtime.Composable
import appText
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun TosPage() {
    Div({
        style {
            property("margin", "${1.r} auto")
            maxWidth(1200.px)
            padding(0.r, 1.r, 1.r, 1.r)
            fontSize(22.px)
            lineHeight("1.5")
            minHeight(100.vh)
        }
    }) {
        H3 {
            appText { tos }
        }
        Div({
            style {
                whiteSpace("pre-wrap")
            }
        }) {
            Text("""
                Platform - The platform these terms apply to.
                You - Any person using the Platform.
                Developers - The developers of the Platform.
                People - All humans and animals
                Account - The account a user uses to engage on the Platform.
                
                By using the Platform, you agree to always edify, lift up, support, and/or celebrate other People.  You agree to refrain from using the Platform for any purposes such as degrading, bringing down, mocking, attacking, or otherwise being hurtful to other People. It is an understanding between you and the Developers that these terms apply to all functions of the Platform, both in public and private discorse. It is an understanding between you and the Developers that engaging in such behavior will cause your Account to be suspended without notice.

                THE SOFTWARE IS PROVIDED “AS IS”, AND "AS AVAILABLE" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
            """.trimIndent())
        }
    }
}
