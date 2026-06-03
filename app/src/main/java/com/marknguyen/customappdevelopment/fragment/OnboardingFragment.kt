package com.marknguyen.customappdevelopment.fragment

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.marknguyen.customappdevelopment.R
import com.marknguyen.customappdevelopment.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    data class Page(val titleRes: String, val bodyRes: String, val animRes: Int)

    private val pages = listOf(
        Page("Search Any City", "Find real-time weather for any Australian city in seconds.", R.raw.lottie_sunny),
        Page("Save Your Favourites", "Bookmark cities to check them instantly from the Saved tab.", R.raw.lottie_cloudy),
        Page("Live Rain Radar", "View live precipitation and cloud layers on the Radar map.", R.raw.lottie_rain)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = OnboardingAdapter(pages)
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnNext.text = if (position == pages.size - 1) "Get Started" else "Next"
            }
        })
        binding.btnNext.setOnClickListener {
            val curr = binding.viewPagerOnboarding.currentItem
            if (curr < pages.size - 1) {
                binding.viewPagerOnboarding.currentItem = curr + 1
            } else {
                markOnboardingDone()
                findNavController().navigate(R.id.action_onboarding_to_home)
            }
        }
    }

    private fun updateDots(pos: Int) {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { i, dot ->
            dot.alpha = if (i == pos) 1f else 0.4f
        }
    }

    private fun markOnboardingDone() {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
    }

    inner class OnboardingAdapter(private val pages: List<Page>) :
        RecyclerView.Adapter<OnboardingAdapter.PageHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int): PageHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageHolder(v)
        }

        override fun onBindViewHolder(holder: PageHolder, pos: Int) = holder.bind(pages[pos])
        override fun getItemCount() = pages.size

        inner class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(page: Page) {
                itemView.findViewById<android.widget.TextView>(R.id.text_onboard_title).text = page.titleRes
                itemView.findViewById<android.widget.TextView>(R.id.text_onboard_body).text = page.bodyRes
                val lottie = itemView.findViewById<LottieAnimationView>(R.id.lottie_onboarding)
                lottie.setAnimation(page.animRes)
                lottie.playAnimation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
