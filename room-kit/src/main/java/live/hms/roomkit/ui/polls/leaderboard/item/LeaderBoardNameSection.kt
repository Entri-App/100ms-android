package live.hms.roomkit.ui.polls.leaderboard.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import live.hms.prebuilt_themes.ApplyRadiusatVertex
import live.hms.roomkit.R
import live.hms.roomkit.databinding.ItemNameSectionBinding
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape
import live.hms.prebuilt_themes.setBackgroundAndColor

class LeaderBoardNameSection(
    private val titleStr: String,
    private val subtitleStr: String,
    private val rankStr: String,
    private val timetakenStr: String? = null,
    private val correctAnswerStr: String? = null,
    private val isSelected: Boolean,
    private val position: ApplyRadiusatVertex = ApplyRadiusatVertex.NONE,
    private val rankBackGroundColor: String? = HMSPrebuiltTheme.getColours()?.secondaryDefault,
) : BindableItem<ItemNameSectionBinding>() {


    override fun bind(viewBinding: ItemNameSectionBinding, position: Int) {
        //themes
        setSelectedView(isSelected, viewBinding)


        with(viewBinding) {
            applyTheme()
            heading.text = titleStr
            subtitle.text = subtitleStr

            if ((rankStr == "0").not())
            rank.text = rankStr
            else
            rank.text = "-"

            if (timetakenStr.isNullOrEmpty().not()) {
                timeTaken.visibility = View.VISIBLE
                timeTaken.text = timetakenStr
            } else {
                timeTaken.visibility = View.GONE
            }

            if (correctAnswerStr.isNullOrEmpty().not()) {
                correctAnswer.visibility = View.VISIBLE
                correctAnswer.text = correctAnswerStr.toString()
            } else {
                correctAnswer.visibility = View.GONE
            }

            if (rankStr == "1")
                trophyicon.visibility = View.VISIBLE
            else
                trophyicon.visibility = View.GONE

            rank.setBackgroundAndColor(
                rankBackGroundColor,
                HMSPrebuiltTheme.getDefaults().secondary_default,
                R.drawable.circle_secondary_80
            )


        }
    }

    private fun setSelectedView(isSelected: Boolean, v: ItemNameSectionBinding) {
        v.rootLayout.background = if (isSelected.not()) {
            getShape(position).apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.backgroundDefault,
                        HMSPrebuiltTheme.getDefaults().background_default
                    )
                )
            }
        } else {
            getShape(position).apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.surfaceDefault,
                        HMSPrebuiltTheme.getDefaults().surface_bright,
                    )
                )
            }
        }
    }


    override fun getLayout(): Int = R.layout.item_name_section



    override fun initializeViewBinding(view: View) = ItemNameSectionBinding.bind(view)

}