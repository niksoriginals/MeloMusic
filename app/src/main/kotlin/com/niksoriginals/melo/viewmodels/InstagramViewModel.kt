package com.niksoriginals.melo.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.niksoriginals.melo.instagram.InstagramNotesManager
import javax.inject.Inject

@HiltViewModel
class InstagramViewModel @Inject constructor(
    val instagramNotesManager: InstagramNotesManager
) : ViewModel()
