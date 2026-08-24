package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.infrastructure.jpa.calendar.RaidCalendarEntryEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface RaidCalendarEntryEntityMapper extends JpaEntityMapper<RaidCalendarEntryEntity, RaidCalendarEntry> {
  @Mapping(target = "startAt", source = "start")
  @Mapping(target = "endAt", source = "end")
  RaidCalendarEntryEntity toEntity(RaidCalendarEntry entry);

  @Mapping(target = "start", source = "startAt")
  @Mapping(target = "end", source = "endAt")
  RaidCalendarEntry toDomain(RaidCalendarEntryEntity entity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "startAt", source = "start")
  @Mapping(target = "endAt", source = "end")
  RaidCalendarEntryEntity partialUpdate(RaidCalendarEntry entry, @MappingTarget RaidCalendarEntryEntity entity);
}
