package org.mtransit.parser.ca_kingston_transit_bus;

import static org.mtransit.commons.RegexUtils.DIGITS;
import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRouteSNToIDConverter;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://openkingston.cityofkingston.ca/explore/dataset/transit-gtfs-routes/
// https://openkingston.cityofkingston.ca/explore/dataset/transit-gtfs-stops/
// RT: https://openkingston.cityofkingston.ca/explore/dataset/transit-gtfs-realtime/
public class KingstonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new KingstonTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Kingston Transit";
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		final String routeLongNameLC = gRoute.getRouteLongNameOrDefault().toLowerCase(Locale.ENGLISH);
		if (routeLongNameLC.contains("out of service")) {
			return EXCLUDE;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		final String tripHeadSignLC = gTrip.getTripHeadsignOrDefault().toLowerCase(Locale.ENGLISH);
		if (tripHeadSignLC.contains("not in service")) {
			return EXCLUDE;
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		switch (routeShortName) {
		case "COV":
			return 99_001L;
		case "XTRA":
			return 99_002L;
		}
		return super.convertRouteIdFromShortNameNotSupported(routeShortName);
	}

	@Override
	public @NotNull String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection deprecation
		return gRoute.getRouteId(); // used for GTFS-RT
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR = "009BC9";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return true;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_PARENTHESIS_ = Pattern.compile("( \\(.*\\))", Pattern.CASE_INSENSITIVE);

	private static final Pattern TRANSFER_POINT_ = Pattern.compile("( transfer (point|pt) (platform|p:)\\d+$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		if (fromStopName) {
			directionHeadSign = ENDS_WITH_PARENTHESIS_.matcher(directionHeadSign).replaceAll(EMPTY);
			directionHeadSign = TRANSFER_POINT_.matcher(directionHeadSign).replaceAll(EMPTY);
		}
		return directionHeadSign;
	}

	private static final Pattern STARTS_WITH_EXPRESS = Pattern.compile("(^(express -) )*", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_EXTRA_BUS = Pattern.compile("(^(extra bus -) )*", Pattern.CASE_INSENSITIVE);

	private static final Pattern KGH_ = CleanUtils.cleanWords("kingston general hosp", "kingston general hospital");
	private static final String KGH_REPLACEMENT = CleanUtils.cleanWordsReplacement("KGH");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_EXPRESS.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = STARTS_WITH_EXTRA_BUS.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = KGH_.matcher(tripHeadsign).replaceAll(KGH_REPLACEMENT);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.SAINT.matcher(tripHeadsign).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern SIDE_ = CleanUtils.cleanWord("side");

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = SIDE_.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String PLACE_CATC = "place_catc";
	private static final String PLACE_CHCA = "place_chca";
	private static final String PLACE_DWNP = "place_dwnp";
	private static final String PLACE_GRDC = "place_grdc";
	private static final String PLACE_KNGC = "place_kngc";
	private static final String PLACE_MSPR = "place_mspr";
	private static final String PLACE_RAIL = "place_rail";

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		return gStop.getStopId(); // used by GTFS-RT
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (!stopId.isEmpty() && CharUtils.isDigitsOnly(stopId)) {
			return Integer.parseInt(stopId); // using stop code as stop ID
		}
		switch (stopId) {
		case PLACE_CATC:
			return 900_000;
		case PLACE_CHCA:
			return 910_000;
		case PLACE_DWNP:
			return 920_000;
		case PLACE_GRDC:
			return 930_000;
		case PLACE_KNGC:
			return 940_000;
		case PLACE_MSPR:
			return 950_000;
		case PLACE_RAIL:
			return 960_000;
		}
		if ("Smspr1".equals(stopId)) {
			return 970000;
		}
		try {
			final Matcher matcher = DIGITS.matcher(stopId);
			if (matcher.find()) {
				final int digits = Integer.parseInt(matcher.group());
				if (stopId.startsWith("S")) {
					return 190_000 + digits;
				}
				throw new MTLog.Fatal("Unexpected stop ID for '%s'!", gStop);
			}
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while finding stop ID for '%s'!", gStop);
		}
		throw new MTLog.Fatal("Unexpected stop ID for '%s'!", gStop);
	}
}
