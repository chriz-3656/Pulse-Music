package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.MusicProvider
import com.example.domain.model.PlayerState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoLedLamp
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoBevelHighlight
import com.example.ui.theme.SkeuoBevelShadow
import com.example.ui.theme.SkeuoCardSurface
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoChromeMid
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoDeckElevated
import com.example.ui.theme.SkeuoLcdBg
import com.example.ui.theme.SkeuoLcdCyan
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.SearchFilter
import com.example.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    playerState: PlayerState,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 10.dp)
            .testTag("search_screen")
    ) {
        // Tactile Recessed Console Search Bar
        SkeuoBevelCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            isRecessed = true,
            shape = RoundedCornerShape(26.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        "Search tracks, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SkeuoTextTertiary
                    )
                },
                leadingIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (uiState.query.isNotBlank()) {
                            viewModel.onSearchSubmitted(uiState.query)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SkeuoAmberGlow
                        )
                    }
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onQueryChanged("") },
                            modifier = Modifier.testTag("search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SkeuoTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        viewModel.onSearchSubmitted(uiState.query)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SkeuoTextPrimary,
                    unfocusedTextColor = SkeuoTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Engine / Provider Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ENGINE:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                ),
                color = SkeuoTextTertiary,
                modifier = Modifier.padding(end = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(MusicProvider.values()) { provider ->
                    val isSelected = uiState.currentProvider == provider
                    SkeuoTactileButton(
                        onClick = { viewModel.setProvider(provider) },
                        modifier = Modifier.height(26.dp),
                        shape = RoundedCornerShape(14.dp),
                        isPressedOrActive = isSelected
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            SkeuoLedLamp(
                                isLit = isSelected,
                                size = 5.dp,
                                color = if (isSelected) SkeuoAmberGlow else SkeuoChromeDark
                            )
                            Text(
                                text = provider.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 9.sp
                                ),
                                color = if (isSelected) SkeuoAmberGlow else SkeuoTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hardware Filter Selector Buttons
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.values()) { filter ->
                val selected = uiState.filter == filter
                SkeuoTactileButton(
                    onClick = { viewModel.setFilter(filter) },
                    isPressedOrActive = selected,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(30.dp).testTag("filter_chip_${filter.name}")
                ) {
                    Text(
                        text = filter.name.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (selected) SkeuoAmberGlow else SkeuoTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        if (uiState.isSearching) {
            LoadingView(message = "SCANNING ARCHIVE...", modifier = Modifier.weight(1f))
        } else if (uiState.errorMessage != null) {
            ErrorView(
                message = uiState.errorMessage ?: "Search failed",
                onRetry = { viewModel.onSearchSubmitted(uiState.query) },
                modifier = Modifier.weight(1f)
            )
        } else if (uiState.query.isNotBlank()) {
            val results = uiState.searchResults
            val hasAnyResults = results.songs.isNotEmpty() || results.artists.isNotEmpty() || results.albums.isNotEmpty() || results.playlists.isNotEmpty() || results.artistTopSongs.isNotEmpty() || results.similarTracks.isNotEmpty() || results.genreRecommendations.isNotEmpty()

            if (uiState.searchSuggestions.isNotEmpty() && !hasAnyResults) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    items(uiState.searchSuggestions) { suggestion ->
                        SkeuoTactileButton(
                            onClick = { 
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.onQueryChanged(suggestion)
                                viewModel.onSearchSubmitted(suggestion)
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp).testTag("suggestion_$suggestion")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SkeuoTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = SkeuoTextPrimary
                                )
                            }
                        }
                    }
                }
            } else if (hasAnyResults) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Direct Matching Tracks Section (Primary search hit)
                    if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.SONGS) && results.songs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Matching Tracks", subtitle = "Results for \"${uiState.query}\"")
                        }
                        items(results.songs, key = { it.id }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongItemRow(
                                song = song,
                                isCurrentTrack = isCurrent,
                                isPlaying = playerState.isPlaying && isCurrent,
                                onClick = { viewModel.playSong(song) },
                                onPlayNext = { viewModel.playNext(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onStartRadio = { viewModel.playSongAsRadio(song) },
                                onDownloadClick = { viewModel.downloadSong(song) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song) }
                            )
                        }
                    }

                    // Discovery Section: Artist's Top Tracks (Spotlight without duplicating direct tracks)
                    val showArtistTop = (uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.DISCOVER || uiState.filter == SearchFilter.SONGS) && results.artistTopSongs.isNotEmpty()
                    if (showArtistTop) {
                        val artistLabel = results.detectedArtistName ?: "Top Artist"
                        item {
                            DiscoverySectionHeader(
                                title = "Top Songs from $artistLabel",
                                subtitle = "Most Streamed & Signature Tracks",
                                badgeText = "ARTIST SPOTLIGHT"
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(results.artistTopSongs, key = { it.id }) { song ->
                                    val isCurrent = playerState.currentSong?.id == song.id
                                    DiscoverySongCard(
                                        song = song,
                                        isCurrent = isCurrent,
                                        isPlaying = playerState.isPlaying && isCurrent,
                                        tagLabel = "POPULAR",
                                        onClick = { viewModel.playSong(song, results.artistTopSongs) }
                                    )
                                }
                            }
                        }
                    }

                    // Discovery Section: Similar Tracks / You Might Enjoy
                    val showSimilar = (uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.DISCOVER) && results.similarTracks.isNotEmpty()
                    if (showSimilar) {
                        item {
                            DiscoverySectionHeader(
                                title = "You Might Enjoy",
                                subtitle = "Sonic resonance matching your search query",
                                badgeText = "ALGORITHMIC PICK"
                            )
                        }
                        items(results.similarTracks, key = { it.id }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongItemRow(
                                song = song,
                                isCurrentTrack = isCurrent,
                                isPlaying = playerState.isPlaying && isCurrent,
                                onClick = { viewModel.playSong(song, results.similarTracks) },
                                onPlayNext = { viewModel.playNext(song) },
                                onAddToQueue = { viewModel.addToQueue(song) },
                                onStartRadio = { viewModel.playSongAsRadio(song) },
                                onDownloadClick = { viewModel.downloadSong(song) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song) }
                            )
                        }
                    }

                    // Discovery Section: Genre & Frequency Exploration
                    val showGenre = (uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.DISCOVER) && results.genreRecommendations.isNotEmpty()
                    if (showGenre) {
                        val genreTitle = results.detectedGenre ?: "Genre Deep Dive"
                        item {
                            DiscoverySectionHeader(
                                title = "Within $genreTitle",
                                subtitle = "Curated exploration tracks within the same musical orbit",
                                badgeText = "EXPLORATION"
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                items(results.genreRecommendations, key = { it.id }) { song ->
                                    val isCurrent = playerState.currentSong?.id == song.id
                                    DiscoverySongCard(
                                        song = song,
                                        isCurrent = isCurrent,
                                        isPlaying = playerState.isPlaying && isCurrent,
                                        tagLabel = "GENRE",
                                        onClick = { viewModel.playSong(song, results.genreRecommendations) }
                                    )
                                }
                            }
                        }
                    }

                    // Discovery Section: Fans Also Liked (Artists)
                    val showFansLiked = (uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.DISCOVER || uiState.filter == SearchFilter.ARTISTS) && results.fansAlsoLikedArtists.isNotEmpty()
                    if (showFansLiked) {
                        item {
                            DiscoverySectionHeader(
                                title = "Fans Also Liked",
                                subtitle = "Listeners exploring this sound also stream these creators",
                                badgeText = "COMMUNITY"
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(results.fansAlsoLikedArtists, key = { it.id }) { artist ->
                                    ArtistSearchCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Artists Section
                    if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.ARTISTS) && results.artists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Featured Artists", subtitle = "Profiles")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(results.artists, key = { it.id }) { artist ->
                                    ArtistSearchCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Albums Section
                    if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.ALBUMS) && results.albums.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Studio Albums", subtitle = "Master Disc Releases")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(results.albums, key = { it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Playlists Section
                    if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.PLAYLISTS) && results.playlists.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Playlists", subtitle = "Collections")
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(results.playlists, key = { it.id }) { playlist ->
                                    PlaylistCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LcdBadge(text = "NO TRACKS FOUND FOR \"${uiState.query.uppercase()}\"", textColor = SkeuoTextSecondary)
                }
            }
        } else {
            // Recent Searches and Discovery Tags
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (uiState.recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT ARCHIVES",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = SkeuoTextPrimary
                            )
                            Text(
                                text = "CLEAR ALL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = SkeuoAmberGlow,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.clearSearchHistory() }
                                    .padding(4.dp)
                                    .testTag("clear_recent_searches")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.recentSearches.forEach { queryText ->
                                SkeuoTactileButton(
                                    onClick = { viewModel.onQueryChanged(queryText) },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = SkeuoAmberGlow,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = queryText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = SkeuoTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteRecentSearch(queryText) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = SkeuoTextTertiary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Popular genres & exploration tags
                item {
                    Text(
                        text = "EXPLORE FREQUENCIES & GENRES",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = SkeuoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val genres = listOf("Aura Synth", "Synthwave", "Cyberpunk", "Lo-Fi Master", "Acoustic Warmth", "Deep Hi-Fi", "Vintage Analog", "Chill Beats")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        genres.forEach { genre ->
                            SkeuoTactileButton(
                                onClick = { viewModel.onQueryChanged(genre) },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = genre.uppercase(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = SkeuoAmberGlow,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSearchCard(
    artist: Artist,
    onClick: () -> Unit
) {
    SkeuoBevelCard(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
            .padding(2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), CircleShape)
            ) {
                AsyncImage(
                    model = artist.imageUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500" },
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ARTIST",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                ),
                color = SkeuoAmberGlow
            )
        }
    }
}

@Composable
private fun DiscoverySectionHeader(
    title: String,
    subtitle: String,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SkeuoAmberGlow.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, SkeuoAmberGlow.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = SkeuoAmberGlow
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = SkeuoTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiscoverySongCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    tagLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkeuoBevelCard(
        modifier = modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag("discovery_card_${song.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Discovery Badge Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tagLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SkeuoAmberGlow
                    )
                }

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Playing" else "Paused",
                            tint = SkeuoAmberGlow,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = if (isCurrent) SkeuoAmberGlow else SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = SkeuoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
