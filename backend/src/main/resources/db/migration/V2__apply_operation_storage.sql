-- Storage for the apply-prepared-structure operation: the fingerprint of the stored structure,
-- and refusal records written when a differently-shaped structure is applied to a season that
-- already holds one.

alter table season_structure
    add column fingerprint text not null;

create table refusal_record (
    id                      bigserial primary key,
    league_id               text not null check (length(league_id) > 0),
    season_id               text not null check (length(season_id) > 0),
    stored_fingerprint      text not null check (length(stored_fingerprint) > 0),
    incoming_fingerprint    text not null check (length(incoming_fingerprint) > 0),
    detected_at             timestamptz not null,
    foreign key (league_id, season_id) references season_structure (league_id, season_id)
);
create index idx_refusal_record_season on refusal_record (league_id, season_id);
