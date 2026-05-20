package ai.ledger.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    var id: UUID,
    @Column(nullable = false, unique = true)
    var email: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
