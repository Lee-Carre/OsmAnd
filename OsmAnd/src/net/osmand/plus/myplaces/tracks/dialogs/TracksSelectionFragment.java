package net.osmand.plus.myplaces.tracks.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment implements UploadGpxListener {

	public static final String TAG = TracksSelectionFragment.class.getSimpleName();

	private ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	private TextView toolbarTitle;
	private ImageButton selectionButton;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarActiveColorId(nightMode);
	}

	@NonNull
	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.track_folder_fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				onBackPressed();
			}
		});
	}

	@Override
	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		super.setRootFolder(rootFolder);
		selectionHelper.clearSelectedItems();
		selectionHelper.setAllItems(rootFolder.getFlattenedTrackItems());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setupToolbar(view);
		}
		updateContent();
		updateSelection();
		return view;
	}

	@Override
	protected void setupAdapter(@NonNull View view) {
		super.setupAdapter(view);
		adapter.setSelectionMode(true);
		adapter.setShouldShowFolder(true);
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getToolbarActiveColor(app, nightMode));
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		setupToolbarActions(view);
	}

	private void setupToolbarActions(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.actions_container);
		container.removeAllViews();

		LayoutInflater inflater = UiUtilities.getInflater(view.getContext(), nightMode);
		setupSelectionButton(inflater, container);
		setupMenuButton(inflater, container);
	}

	private void setupSelectionButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		selectionButton = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		selectionButton.setOnClickListener(v -> {
			if (selectionHelper.isAllItemsSelected()) {
				selectionHelper.clearSelectedItems();
			} else {
				selectionHelper.selectAllItems();
			}
			updateSelection();
			adapter.notifyDataSetChanged();
		});
		updateSelection();
		container.addView(selectionButton);
	}

	private void setupMenuButton(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white));
		button.setOnClickListener(v -> {
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null) {
				Set<TrackItem> trackItems = selectionHelper.getSelectedItems();
				foldersHelper.showItemsOptionsMenu(trackItems, v, this);
			}
		});
		button.setContentDescription(getString(R.string.shared_string_more));
		container.addView(button);
	}

	private void updateSelection() {
		updateToolbar();
		boolean selected = selectionHelper.isAllItemsSelected();
		int iconId = selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
		selectionButton.setImageDrawable(getIcon(iconId));
		selectionButton.setContentDescription(getString(selected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));
	}

	@Override
	public void updateContent() {
		super.updateContent();
		updateToolbar();
	}

	private void updateToolbar() {
		toolbarTitle.setText(String.valueOf(selectionHelper.getSelectedItemsSize()));
	}

	@Override
	public void onResume() {
		super.onResume();
		updateActionBar(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		updateActionBar(true);
	}

	private void onBackPressed() {
		if (rootFolder.equals(selectedFolder)) {
			dismiss();
		} else {
			selectedFolder = selectedFolder.getParentFolder();
			updateContent();
		}
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return selectionHelper.isItemSelected(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		selectionHelper.onItemsSelected(trackItems, selected);
		adapter.onItemsSelected(trackItems);
		updateSelection();
	}

	@Override
	public boolean isTracksGroupSelected(@NonNull TracksGroup group) {
		if (group instanceof TrackFolder) {
			TrackFolder folder = (TrackFolder) group;
			List<TrackItem> trackItems = folder.getFlattenedTrackItems();
			return !trackItems.isEmpty() && selectionHelper.isItemsSelected(trackItems);
		}
		return selectionHelper.isItemsSelected(group.getTrackItems());
	}

	@Override
	public void onTracksGroupSelected(@NonNull TracksGroup group, boolean selected) {
		if (group instanceof TrackFolder) {
			TrackFolder folder = (TrackFolder) group;
			selectionHelper.onItemsSelected(folder.getFlattenedTrackItems(), selected);
		} else {
			selectionHelper.onItemsSelected(group.getTrackItems(), selected);
		}
		adapter.onItemsSelected(Collections.singleton(group));
		updateSelection();
	}

	@Override
	protected boolean shouldShowFolderStats() {
		return false;
	}

	@Override
	public void onGpxUploaded(String result) {
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder trackFolder, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.setRetainInstance(true);
			fragment.setRootFolder(trackFolder);
			fragment.setSelectedFolder(trackFolder);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
