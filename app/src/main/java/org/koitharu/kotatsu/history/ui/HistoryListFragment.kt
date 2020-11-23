package org.koitharu.kotatsu.history.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_list.*
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.utils.ext.ellipsize

class HistoryListFragment : MangaListFragment() {

	override val viewModel by viewModel<HistoryListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.onItemRemoved.observe(viewLifecycleOwner, ::onItemRemoved)
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.opt_history, menu)
		super.onCreateOptionsMenu(menu, inflater)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_clear_history -> {
				MaterialAlertDialogBuilder(context ?: return false)
					.setTitle(R.string.clear_history)
					.setMessage(R.string.text_clear_history_prompt)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.clear) { _, _ ->
						viewModel.clearHistory()
					}.show()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun getTitle(): CharSequence? {
		return getString(R.string.history)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(R.string.text_history_holder)
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	override fun onCreatePopupMenu(inflater: MenuInflater, menu: Menu, data: Manga) {
		super.onCreatePopupMenu(inflater, menu, data)
		inflater.inflate(R.menu.popup_history, menu)
	}

	override fun onPopupMenuItemSelected(item: MenuItem, data: Manga): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.removeFromHistory(data)
				true
			}
			else -> super.onPopupMenuItemSelected(item, data)
		}
	}

	fun onItemRemoved(item: Manga) {
		Snackbar.make(
			recyclerView, getString(
				R.string._s_removed_from_history,
				item.title.ellipsize(16)
			), Snackbar.LENGTH_SHORT
		).show()
	}

	companion object {

		fun newInstance() = HistoryListFragment()
	}
}