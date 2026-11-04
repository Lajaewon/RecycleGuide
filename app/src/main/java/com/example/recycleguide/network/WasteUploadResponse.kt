package com.example.recycleguide.network

data class WasteUploadResponse(
    val success: Boolean,
    val filename: String,
    val originalFilename: String?,
    val contentType: String?,
    val sizeBytes: Long,
    val message: String
)