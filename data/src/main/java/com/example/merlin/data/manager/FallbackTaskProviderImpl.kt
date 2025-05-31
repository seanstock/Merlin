package com.example.merlin.data.manager

import android.util.Log
import com.example.merlin.data.database.entities.ChildProfile

/**
 * Implementation of FallbackTaskProvider that provides educational tasks when AI is unavailable.
 * Ensures the app remains functional even without internet connectivity.
 */
class FallbackTaskProviderImpl : FallbackTaskProvider {
    
    companion object {
        private const val TAG = "FallbackTaskProviderImpl"
    }

    private val fallbackTasks = listOf(
        "Let's practice counting! Can you count from 1 to 10?",
        "What's your favorite color? Can you name three things that are that color?",
        "Let's practice the alphabet! Can you say the letters A through E?",
        "Can you tell me about your family? Who lives in your house?",
        "What did you have for breakfast today? Was it yummy?",
        "Let's practice shapes! Can you find something round in the room?",
        "What's your favorite animal? What sound does it make?",
        "Can you clap your hands 5 times? Let's count together!",
        "What's the weather like today? Is it sunny, cloudy, or rainy?",
        "Let's practice saying 'please' and 'thank you'! When do we use these words?",
        "Can you name three different fruits? Which one is your favorite?",
        "Let's practice big and small! Can you find something big and something small?",
        "What makes you happy? Tell me about something that makes you smile!",
        "Can you hop on one foot? Let's try it together!",
        "What's your favorite book? Can you tell me about it?"
    )

    private val ageSpecificTasks = mapOf(
        3 to listOf(
            "Can you show me how you brush your teeth?",
            "Let's practice saying your name! What's your full name?",
            "Can you point to your nose, eyes, and mouth?",
            "What sound does a cow make? How about a dog?",
            "Can you walk in a circle? Let's try it!"
        ),
        4 to listOf(
            "Can you count to 15? Let's do it together!",
            "What comes after the number 5? How about after 8?",
            "Can you name the days of the week? Let's start with Monday!",
            "Let's practice rhyming! What rhymes with 'cat'?",
            "Can you draw a circle and a square?"
        ),
        5 to listOf(
            "Can you spell your first name? Let's go letter by letter!",
            "What's 2 + 2? How about 3 + 1?",
            "Can you tell me the months of the year? Let's start with January!",
            "Let's practice reading! Can you sound out the word 'dog'?",
            "What's the difference between left and right? Show me your left hand!"
        ),
        6 to listOf(
            "Can you count by 2s? Let's try: 2, 4, 6...",
            "What's 5 + 3? How about 10 - 2?",
            "Can you read this sentence: 'The cat is big'?",
            "Let's practice writing! Can you write the letter 'A'?",
            "What are the four seasons? Which one is your favorite?"
        ),
        7 to listOf(
            "Can you count by 5s up to 50?",
            "What's 6 + 7? How about 15 - 8?",
            "Can you read a short story and tell me what happened?",
            "Let's practice cursive! Can you write your name in cursive?",
            "What are the seven days of the week in order?"
        ),
        8 to listOf(
            "Can you solve 12 + 15? How about 20 - 7?",
            "Let's practice multiplication! What's 3 × 4?",
            "Can you write a short paragraph about your favorite hobby?",
            "What are the planets in our solar system? Can you name three?",
            "Let's practice fractions! What's half of 8?"
        )
    )

    /**
     * Get a fallback task appropriate for the child's profile.
     */
    override fun getTaskForChild(childProfile: ChildProfile): String {
        Log.d(TAG, "Getting fallback task for child: ${childProfile.name}, age: ${childProfile.age}")
        
        val age = childProfile.age ?: 6
        val ageTasks = ageSpecificTasks[age] ?: ageSpecificTasks[6] ?: emptyList()
        
        // Combine age-specific tasks with general tasks
        val availableTasks = ageTasks + fallbackTasks
        
        // Return a random task
        val selectedTask = availableTasks.random()
        
        Log.d(TAG, "Selected fallback task: $selectedTask")
        return selectedTask
    }

    /**
     * Get a random general fallback task.
     */
    fun getRandomTask(): String {
        val selectedTask = fallbackTasks.random()
        Log.d(TAG, "Selected random fallback task: $selectedTask")
        return selectedTask
    }

    /**
     * Get multiple fallback tasks for variety.
     */
    fun getMultipleTasks(count: Int, childProfile: ChildProfile? = null): List<String> {
        val age = childProfile?.age ?: 6
        val ageTasks = ageSpecificTasks[age] ?: ageSpecificTasks[6] ?: emptyList()
        val availableTasks = (ageTasks + fallbackTasks).shuffled()
        
        return availableTasks.take(count.coerceAtMost(availableTasks.size))
    }

    /**
     * Get a motivational message for when AI is unavailable.
     */
    fun getMotivationalMessage(): String {
        val messages = listOf(
            "Don't worry! We can still learn together even without the internet!",
            "Let's have fun with some offline learning activities!",
            "I have lots of fun tasks we can do right now!",
            "Learning never stops! Let's try something new!",
            "Even without the internet, we can still explore and discover!"
        )
        
        return messages.random()
    }

    /**
     * Get an encouraging response for completed tasks.
     */
    fun getEncouragementMessage(): String {
        val messages = listOf(
            "Great job! You're doing amazing!",
            "Wonderful! Keep up the excellent work!",
            "Fantastic! I'm so proud of you!",
            "Awesome! You're such a good learner!",
            "Brilliant! You're getting better and better!",
            "Super! That was really well done!",
            "Excellent! You should be proud of yourself!",
            "Amazing! You're working so hard!",
            "Perfect! You're doing such a great job!",
            "Outstanding! Keep being awesome!"
        )
        
        return messages.random()
    }
} 