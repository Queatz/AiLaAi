package com.queatz.db

fun Db.comment(id: String) = query(
    CommentExtended::class,
    """
        let comment = document(@to)
        return {
            ${f(CommentExtended::comment)}: comment,
            ${f(CommentExtended::person)}: keep(document(comment._from), "_key", "${f(Person::name)}", "${f(Person::photo)}", "${f(Person::seen)}"),
            ${f(CommentExtended::totalReplies)}: count(
                for x in inbound @to graph `${Comment::class.graph()}`
                    return true
            ),
            ${f(CommentExtended::replies)}: (
                for commentReplyPerson, commentReply in inbound @to graph `${Comment::class.graph()}`
                    sort commentReply.${f(Comment::createdAt)} desc
                    return {
                        ${f(CommentExtended::comment)}: commentReply,
                        ${f(CommentExtended::person)}: keep(commentReplyPerson, "_key", "${f(Person::name)}", "${f(Person::photo)}", "${f(Person::seen)}"),
                        ${f(CommentExtended::totalReplies)}: count(
                            for x in inbound commentReply graph `${Comment::class.graph()}`
                                return true
                        )
                    }
            )
        }
    """.trimIndent(),
    mapOf(
        "to" to id.asId(Comment::class)
    )
).firstOrNull()

fun Db.commentsOf(to: String) = query(
    CommentExtended::class,
    """
        for person, comment in inbound @to graph `${Comment::class.graph()}`
            sort comment.${f(Comment::createdAt)} desc
            return {
                ${f(CommentExtended::comment)}: comment,
                ${f(CommentExtended::person)}: keep(person, "_key", "${f(Person::name)}", "${f(Person::photo)}", "${f(Person::seen)}"),
                ${f(CommentExtended::totalReplies)}: count(
                    for x in inbound comment graph `${Comment::class.graph()}`
                        return true
                ),
                ${f(CommentExtended::replies)}: (
                    for commentReplyPerson, commentReply in inbound comment graph `${Comment::class.graph()}`
                        sort commentReply.${f(Comment::createdAt)} desc
                        return {
                            ${f(CommentExtended::comment)}: commentReply,
                            ${f(CommentExtended::person)}: keep(commentReplyPerson, "_key", "${f(Person::name)}", "${f(Person::photo)}", "${f(Person::seen)}"),
                            ${f(CommentExtended::totalReplies)}: count(
                                for x in inbound commentReply graph `${Comment::class.graph()}`
                                    return true
                            )
                        }
                )
            }
    """.trimIndent(),
    mapOf(
        "to" to to
    )
)
