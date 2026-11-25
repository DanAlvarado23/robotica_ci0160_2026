package com.example.leitroboter.data

data class MapPoint(val x: Float, val y: Float)

data class MapRoute(val label: String, val points: List<MapPoint>)
