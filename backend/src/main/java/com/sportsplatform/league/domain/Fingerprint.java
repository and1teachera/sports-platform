package com.sportsplatform.league.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The one value by which an incoming prepared structure is judged identical to the stored one.
 *
 * <p>Computed as a lowercase hexadecimal SHA-256 digest over a canonical, newline-delimited
 * textual form of the structural facts of a {@link SeasonStructure}: the (league, season) pair,
 * the competitions, their phases, the Cup groups under group-play phases, the season-scoped
 * placement of each club (with conference and division), and the competition participations
 * (with their optional cup-group assignment). Lines are prefixed by a one-character record kind
 * so that structurally unrelated values can never collide.</p>
 *
 * <p>Deliberately excluded from the fingerprint, permanently: a club's display attributes
 * (name, abbreviation), the predecessor and succession link, the clubs' provider references and
 * all correlation, and any non-structural display metadata of the league. Changing what the
 * fingerprint covers requires an explicit fingerprint-version decision and a migration that
 * recomputes every stored fingerprint; this is the final definition until that pair happens
 * together.</p>
 *
 * <p>Determinism rests on the canonical order {@link SeasonStructure} enforces in its
 * constructor (competitions by id, phases by ordinal, groups by id, placements by club id,
 * participations by competition then club), so the same facts in a different input order
 * produce the same fingerprint.</p>
 */
public record Fingerprint(String value) {

    public Fingerprint {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Fingerprint value must not be blank");
        }
    }

    public static Fingerprint of(SeasonStructure structure) {
        Objects.requireNonNull(structure, "structure");
        return new Fingerprint(sha256Hex(canonicalForm(structure)));
    }

    static String canonicalForm(SeasonStructure structure) {
        List<String> lines = new ArrayList<>();
        lines.add("S|" + structure.league().value() + "|" + structure.season().value());

        for (Competition competition : structure.competitions()) {
            lines.add("C|" + competition.id().value() + "|" + competition.name());
            for (Phase phase : competition.phases()) {
                lines.add("P|" + competition.id().value()
                        + "|" + phase.ordinal()
                        + "|" + phase.id().value()
                        + "|" + phase.name());
                for (CupGroup group : phase.groups()) {
                    lines.add("G|" + competition.id().value()
                            + "|" + phase.id().value()
                            + "|" + group.id().value()
                            + "|" + group.name());
                }
            }
        }

        for (SeasonPlacement placement : structure.placements()) {
            lines.add("L|" + placement.club().value()
                    + "|" + placement.conference().name()
                    + "|" + placement.division().name());
        }

        for (CompetitionParticipation participation : structure.participations()) {
            lines.add("X|" + participation.competition().value()
                    + "|" + participation.club().value()
                    + "|" + (participation.cupGroup() == null
                            ? ""
                            : participation.cupGroup().value()));
        }

        return String.join("\n", lines);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
