package com.chamchamcham.application.farm

import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate
import com.chamchamcham.domain.farm.FarmDataSource
import com.chamchamcham.domain.member.Member
import java.math.BigDecimal
import java.util.UUID

object FarmCommand {
    data class Create(
        val memberId: UUID,
        val draft: Draft,
        val cropIds: List<UUID>
    )

    data class Replace(
        val memberId: UUID,
        val farmId: UUID,
        val draft: Draft,
        val cropIds: List<UUID>
    )

    data class Delete(
        val memberId: UUID,
        val farmId: UUID
    )

    data class Draft(
        val name: String,
        val roadAddress: String,
        val jibunAddress: String?,
        val latitude: Double,
        val longitude: Double,
        val pnu: String?,
        val landCategory: String?,
        val areaSqm: BigDecimal?,
        val areaIsManualEntry: Boolean,
        val boundaryCoordinates: List<BoundaryCoordinate>,
        val dataSource: DataSource
    )

    data class BoundaryCoordinate(
        val latitude: Double,
        val longitude: Double
    )

    data class DataSource(
        val address: String?,
        val coordinate: String?,
        val parcel: String?,
        val landCharacteristic: String?
    )
}

fun FarmCommand.Draft.toFarm(owner: Member): Farm =
    Farm(
        null,
        owner,
        name,
        roadAddress,
        jibunAddress,
        latitude,
        longitude,
        pnu,
        landCategory,
        areaSqm,
        areaIsManualEntry,
        boundaryCoordinates.map {
            FarmBoundaryCoordinate(it.latitude, it.longitude)
        }.toMutableList(),
        dataSource.toDomain()
    )

fun Farm.apply(draft: FarmCommand.Draft) {
    updateProfile(
        draft.name,
        draft.roadAddress,
        draft.jibunAddress,
        draft.latitude,
        draft.longitude,
        draft.pnu,
        draft.landCategory,
        draft.areaSqm,
        draft.areaIsManualEntry,
        draft.boundaryCoordinates.map {
            FarmBoundaryCoordinate(it.latitude, it.longitude)
        },
        draft.dataSource.toDomain()
    )
}

private fun FarmCommand.DataSource.toDomain(): FarmDataSource =
    FarmDataSource(address, coordinate, parcel, landCharacteristic)
