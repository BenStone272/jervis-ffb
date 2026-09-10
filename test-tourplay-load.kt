import com.jervisffb.engine.bb2025.rules2025.StandardBB2025Rules
import com.jervisffb.tourplay.TourPlayApi
import com.jervisffb.utils.runBlocking

fun main() = runBlocking {
    val api = TourPlayApi()
    val rules = StandardBB2025Rules()
    
    println("Loading roster 233461...")
    val result = api.loadRoster(233461L, rules)
    
    when {
        result.isSuccess -> {
            val teamFile = result.getOrNull()
            println("✓ Successfully loaded!")
            println("Team: ${teamFile?.team?.name}")
            println("Players: ${teamFile?.team?.players?.size}")
        }
        result.isFailure -> {
            val error = result.exceptionOrNull()
            println("✗ Failed to load!")
            println("Error: ${error?.message}")
            println("Stack trace:")
            error?.printStackTrace()
        }
    }
}
