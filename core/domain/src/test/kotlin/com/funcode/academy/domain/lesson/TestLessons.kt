package com.funcode.academy.domain.lesson

/** Utilidades para construir lecciones y contenido en las pruebas. */
internal object TestLessons {

    fun exercise(id: String, content: ExerciseContent, hints: List<String> = listOf("Pista 1", "Pista 2")) = Exercise(
        id = id, objectiveIds = listOf("o1"), difficulty = 1, prompt = "Enunciado", content = content,
        hints = hints, explanation = "Explicación", incorrectFeedback = "Inténtalo de nuevo",
    )

    fun lesson(vararg exercises: Exercise, assessment: Assessment? = null, kind: LessonKind = LessonKind.LESSON) = Lesson(
        id = "test.m01.l01", moduleId = "test.m01", languageId = "java", locale = "es", version = 1,
        reviewStatus = ReviewStatus.DRAFT, kind = kind, title = "Título", description = "Descripción",
        objectives = listOf(LearningObjective("o1", "Objetivo")),
        presentations = mapOf(PresentationVariant.DEFAULT to listOf(Section.Text("Teoría"))),
        exercises = exercises.toList(), assessment = assessment,
    )

    val trueFalse = ExerciseContent.TrueFalse("Afirmación", correct = true)

    /** JSON mínimo válido; [exercises] y [extra] permiten variarlo en cada prueba. */
    fun json(
        exercises: String = """[{"id":"t.e1","type":"trueFalse","objectiveIds":["o1"],"difficulty":1,
            "prompt":"¿V o F?","statement":"S","correct":true,"hints":["h"],
            "explanation":"E","incorrectFeedback":"I"}]""",
        extra: String = "",
        presentations: String = """{"default":[{"type":"text","text":"Teoría"}]}""",
    ) = """{"schemaVersion":1,"id":"t.l1","moduleId":"t.m1","languageId":"java","locale":"es","version":1,
        "reviewStatus":"DRAFT","title":"T","description":"D","objectives":[{"id":"o1","text":"O"}],
        "presentations":$presentations,"exercises":$exercises $extra}"""

    /** Respuesta correcta derivada del propio contenido (para comprobar su coherencia). */
    fun correctAnswerFor(content: ExerciseContent): Answer = when (content) {
        is ExerciseContent.MultipleChoice -> Answer.Choice(content.correctIndices)
        is ExerciseContent.TrueFalse -> Answer.TrueFalse(content.correct)
        is ExerciseContent.FillInBlanks -> Answer.Blanks(content.acceptedAnswers.map { it.first() })
        is ExerciseContent.OrderLines -> Answer.Order(content.lines.indices.toList())
        is ExerciseContent.PredictOutput -> Answer.Output(content.acceptedOutputs.first())
        is ExerciseContent.FindError -> Answer.ErrorLines(content.errorLines)
        is ExerciseContent.MatchPairs -> Answer.Pairs(content.correctPairs)
    }
}
