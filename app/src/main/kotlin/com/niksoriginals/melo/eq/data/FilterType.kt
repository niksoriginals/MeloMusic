package com.niksoriginals.melo.eq.data

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    
    PK,
    
    LSC,
    
    HSC,
    
    LPQ,
    
    HPQ
}