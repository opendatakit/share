/*
Copyright 2017 Shobhit
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.odk.share.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import butterknife.BindView;
import org.odk.share.R;
import org.odk.share.adapters.FormsAdapter;
import org.odk.share.adapters.InstanceAdapter;
import org.odk.share.adapters.SortDialogAdapter;
import org.odk.share.utilities.ArrayUtils;

import java.util.LinkedHashSet;

import static org.odk.share.utilities.ApplicationConstants.SortingOrder.BY_NAME_ASC;

abstract class AppListFragment extends InjectableFragment {

    protected String[] sortingOptions;
    protected LinkedHashSet<Long> selectedInstances = new LinkedHashSet<>();
    private Integer selectedSortingOrder;
    private BottomSheetDialog bottomSheetDialog;
    private String filterText;
    public FormsAdapter formAdapter;
    public InstanceAdapter instanceAdapter;

    private static final String SELECTED_INSTANCES_KEY = "ROTATION_SELECTED_INSTANCES";
    private static final String TOGGLE_BUTTON_STATUS_KEY = "TOGGLE_BUTTON_STATUS";
    private static final String TOGGLE_BUTTON_TEXT_KEY = "TOGGLE_BUTTON_TEXT";

    @BindView(R.id.send_button)
    Button sendButton;
    @BindView(R.id.toggle_button)
    Button toggleButton;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLongArray(SELECTED_INSTANCES_KEY, ArrayUtils.toPrimitive(
                selectedInstances.toArray(new Long[selectedInstances.size()])));
        outState.putBoolean(TOGGLE_BUTTON_STATUS_KEY, toggleButton.isEnabled());
        outState.putString(TOGGLE_BUTTON_TEXT_KEY, toggleButton.getText().toString());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.list_menu, menu);

        final MenuItem sortItem = menu.findItem(R.id.menu_sort);
        final MenuItem searchItem = menu.findItem(R.id.menu_filter);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        SearchView.SearchAutoComplete searchAutoComplete =
                (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setCursorVisible(true);
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
        searchAutoComplete.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterText = query;
                updateAdapter();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterText = newText;
                updateAdapter();
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                sortItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                sortItem.setVisible(true);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort:
                bottomSheetDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performSelectedSearch(int position) {
        saveSelectedSortingOrder(position);
        updateAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bottomSheetDialog == null) {
            setupBottomSheet();
        }
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            long[] previousSelectedInstances = savedInstanceState.getLongArray(SELECTED_INSTANCES_KEY);
            for (long previousSelectedInstance : previousSelectedInstances) {
                selectedInstances.add(previousSelectedInstance);
            }
            sendButton.setEnabled(selectedInstances.size() > 0);
            toggleButton.setEnabled(savedInstanceState.getBoolean(TOGGLE_BUTTON_STATUS_KEY));
            toggleButton.setText(savedInstanceState.getString(TOGGLE_BUTTON_TEXT_KEY));
        }
    }


    private void setupBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(getActivity());
        View sheetView = getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        final RecyclerView recyclerView = sheetView.findViewById(R.id.recyclerView);

        sortingOptions = new String[]{
                getString(R.string.sort_by_name_asc), getString(R.string.sort_by_name_desc),
                getString(R.string.sort_by_date_asc), getString(R.string.sort_by_date_desc)
        };
        final SortDialogAdapter adapter = new SortDialogAdapter(getActivity(), recyclerView, sortingOptions, getSelectedSortingOrder(), new SortDialogAdapter.RecyclerViewClickListener() {
            @Override
            public void onItemClicked(SortDialogAdapter.ViewHolder holder, int position) {
                holder.updateItemColor(selectedSortingOrder);
                performSelectedSearch(position);
                bottomSheetDialog.dismiss();
            }
        });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        bottomSheetDialog.setContentView(sheetView);
    }

    protected abstract void updateAdapter();

    protected abstract String getSortingOrderKey();

    private void saveSelectedSortingOrder(int selectedStringOrder) {
        selectedSortingOrder = selectedStringOrder;
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putInt(getSortingOrderKey(), selectedStringOrder)
                .apply();
    }

    protected void restoreSelectedSortingOrder() {
        selectedSortingOrder = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getInt(getSortingOrderKey(), BY_NAME_ASC);
    }

    protected int getSelectedSortingOrder() {
        if (selectedSortingOrder == null) {
            restoreSelectedSortingOrder();
        }
        return selectedSortingOrder;
    }

    protected CharSequence getFilterText() {
        return filterText != null ? filterText : "";
    }
}
