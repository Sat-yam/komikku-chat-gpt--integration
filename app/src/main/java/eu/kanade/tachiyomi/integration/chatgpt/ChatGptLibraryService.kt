package eu.kanade.tachiyomi.integration.chatgpt

// KMK -->
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.manga.interactor.GetLibraryManga

/**
 * Read-only application facade for the first ChatGPT integration milestone.
 *
 * This deliberately uses Komikku's domain interactors and never exposes
 * SQLDelight queries, database handles, extension internals or credentials.
 */
class ChatGptLibraryService(
    private val getLibraryManga: GetLibraryManga,
    private val getCategories: GetCategories,
    private val getChaptersByMangaId: GetChaptersByMangaId,
) {

    suspend fun getLibrary(): List<ChatGptLibraryEntry> {
        val categoryNames = getCategories.await().associate { it.id to it.name }

        return getLibraryManga.await().map { libraryItem ->
            val manga = libraryItem.manga
            ChatGptLibraryEntry(
                mangaId = manga.id,
                title = manga.title,
                sourceId = manga.source,
                mangaUrl = manga.url,
                author = manga.author,
                artist = manga.artist,
                description = manga.description,
                genres = manga.genre.orEmpty(),
                status = manga.status,
                coverUrl = manga.thumbnailUrl,
                totalChapters = libraryItem.totalChapters,
                readChapters = libraryItem.readCount,
                unreadChapters = libraryItem.unreadCount,
                categoryIds = libraryItem.categories,
                categoryNames = libraryItem.categories.mapNotNull(categoryNames::get),
                addedAt = manga.dateAdded,
                lastReadAt = libraryItem.lastRead,
            )
        }
    }

    /**
     * Returns chapter metadata only for manga currently present in the library.
     * Chapter page URLs and local file paths are intentionally not exposed.
     */
    suspend fun getChapters(mangaId: Long): List<ChatGptChapterEntry> {
        val isInLibrary = getLibraryManga.await().any { it.id == mangaId }
        require(isInLibrary) { "Manga is not in the Komikku library" }

        return getChaptersByMangaId.await(mangaId).map { chapter ->
            ChatGptChapterEntry(
                chapterId = chapter.id,
                mangaId = chapter.mangaId,
                name = chapter.name,
                chapterNumber = chapter.chapterNumber,
                isRead = chapter.read,
                isBookmarked = chapter.bookmark,
                lastPageRead = chapter.lastPageRead,
                uploadedAt = chapter.dateUpload,
                scanlator = chapter.scanlator,
            )
        }
    }
}

data class ChatGptLibraryEntry(
    val mangaId: Long,
    val title: String,
    val sourceId: Long,
    val mangaUrl: String,
    val author: String?,
    val artist: String?,
    val description: String?,
    val genres: List<String>,
    val status: Long,
    val coverUrl: String?,
    val totalChapters: Long,
    val readChapters: Long,
    val unreadChapters: Long,
    val categoryIds: List<Long>,
    val categoryNames: List<String>,
    val addedAt: Long,
    val lastReadAt: Long,
)

data class ChatGptChapterEntry(
    val chapterId: Long,
    val mangaId: Long,
    val name: String,
    val chapterNumber: Double,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val lastPageRead: Long,
    val uploadedAt: Long,
    val scanlator: String?,
)
// KMK <--
