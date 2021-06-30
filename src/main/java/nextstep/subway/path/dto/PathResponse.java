package nextstep.subway.path.dto;

import nextstep.subway.line.domain.Line;
import nextstep.subway.path.domain.Path;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;

import java.util.List;
import java.util.stream.Collectors;

public class PathResponse {
    private List<StationResponse> stations;
    private int distance;
    private int fare;

    public PathResponse(){}

    public PathResponse(List<StationResponse> stations, int distance, int fare) {
        this.stations = stations;
        this.distance = distance;
        this.fare = fare;
    }

    public static PathResponse of(Path path, List<Line> lines, int age) {
        List<Station> stations = path.findShortPath(lines);
        int distance = path.calculateDistance(lines.size());
        return new PathResponse(getStations(stations), distance, path.calculateFare(distance, age));
    }

    public List<StationResponse> getStations() {
        return stations;
    }

    public int getDistance() {
        return distance;
    }

    public int getFare() {
        return fare;
    }

    private static List<StationResponse> getStations(List<Station> station) {
        return station.stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());
    }
}
