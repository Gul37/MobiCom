package com.example.mobicom;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class LogFragment extends Fragment {

    private void log(String method) {
        Log.d("MobiComLifecycle", getClass().getSimpleName() + "." + method);
    }

    @Override public void onAttach(@NonNull Context c) { super.onAttach(c); log("onAttach"); }
    @Override public void onCreate(@Nullable Bundle b) { super.onCreate(b); log("onCreate"); }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) { super.onViewCreated(v, b); log("onViewCreated"); }
    @Override public void onStart() { super.onStart(); log("onStart"); }
    @Override public void onResume() { super.onResume(); log("onResume"); }
    @Override public void onPause() { super.onPause(); log("onPause"); }
    @Override public void onStop() { super.onStop(); log("onStop"); }
    @Override public void onDestroyView() { super.onDestroyView(); log("onDestroyView"); }
    @Override public void onDestroy() { super.onDestroy(); log("onDestroy"); }
    @Override public void onDetach() { super.onDetach(); log("onDetach"); }
}
