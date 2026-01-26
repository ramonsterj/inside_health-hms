package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "rooms")
@SQLRestriction("deleted_at IS NULL")
class Room(

    @Column(nullable = false, unique = true, length = 50)
    var number: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var type: RoomType,

    @Column(nullable = false)
    var capacity: Int = 1,

) : BaseEntity()
