package com.example.alphamusic.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alphamusic.core.ui.components.shimmerEffect

@Composable
fun HomeShimmer() {
    LazyColumn(
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, 
            bottom = 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header Shimmer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shimmerEffect(RoundedCornerShape(12.dp))
                )
            }
            Spacer(modifier = Modifier.height(12.dp)) // Matching reduced spacer from HomeScreen
        }

        // Quick Picks Shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 12.dp)
                    .width(120.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(2) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) {
                            TrackItemShimmer(modifier = Modifier.width(300.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Albums Shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 12.dp)
                    .width(150.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(3) {
                    LargeTrackCardShimmer()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Trending Shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, bottom = 12.dp)
                    .width(100.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(3) {
                    LargeTrackCardShimmer()
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LargeTrackCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(140.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shimmerEffect(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .shimmerEffect()
        )
    }
}

@Composable
fun TrackItemShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(55.dp) 
                .shimmerEffect(RoundedCornerShape(5.dp)) 
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp) 
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .shimmerEffect()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .shimmerEffect(RoundedCornerShape(12.dp))
        )
    }
}
