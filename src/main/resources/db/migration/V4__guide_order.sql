-- Keep strategy guides in the order they were added.
ALTER TABLE event_strategy_entity_guides
    ADD COLUMN guide_order INT;

UPDATE event_strategy_entity_guides g
SET guide_order = numbered.position
FROM (SELECT ctid, ROW_NUMBER() OVER (PARTITION BY owner_id ORDER BY guide) - 1 AS position
      FROM event_strategy_entity_guides) numbered
WHERE g.ctid = numbered.ctid;

ALTER TABLE event_strategy_entity_guides
    ALTER COLUMN guide_order SET NOT NULL;
