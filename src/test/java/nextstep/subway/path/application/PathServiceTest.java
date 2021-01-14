package nextstep.subway.path.application;

import nextstep.subway.common.exception.CustomException;
import nextstep.subway.line.application.LineService;
import nextstep.subway.line.domain.Line;
import nextstep.subway.path.dto.PathRequest;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("경로에 관련한 기능")
@ExtendWith(MockitoExtension.class)
class PathServiceTest {
    @Mock
    private LineService lineService;
    @Mock
    private StationService stationService;

    private PathService pathService;

    private Station 강남역;
    private Station 양재역;
    private Station 교대역;
    private Station 남부터미널;
    private Line 이호선;
    private Line 신분당선;
    private Line 삼호선;

    @BeforeEach
    void beforeEach() {
        pathService = new PathService(stationService, lineService);
        강남역 = new Station("강남역");
        양재역 = new Station("양재역");
        교대역 = new Station("교대역");
        남부터미널 = new Station("남부터미널");
        이호선 = new Line("2호선", "bg-green-200", 교대역, 강남역, 1000L);
        삼호선 = new Line("3호선", "bg-yellow-200", 교대역, 양재역, 500L);
        삼호선.addSection(교대역, 남부터미널, 300L);
        신분당선 = new Line("신분당선", "bg-red-200", 강남역, 양재역, 1000L);
    }

    @DisplayName("최단 경로 찾기")
    @Test
    void findPath() {
        // Given
        given(lineService.findAllLines()).willReturn(Arrays.asList(이호선, 삼호선, 신분당선));
        given(stationService.findById(any())).willReturn(교대역).willReturn(양재역);
        // When
        PathResponse shortestPath = pathService.findPath(new PathRequest(교대역.getId(), 양재역.getId()));
        // Then
        assertAll(
                () -> assertThat(shortestPath.getStations()).isNotNull(),
                () -> assertThat(shortestPath.getStations()).hasSize(3)
                        .extracting(StationResponse::getName)
                        .containsExactly("교대역", "남부터미널", "양재역"),
                () -> assertThat(shortestPath.getDistance()).isEqualTo(500L)
        );
    }

    @DisplayName("예외 상황 - 출발역과 도착역이 같은 경우")
    @Test
    void exceptionToFindShortestPathOfSameSourceAndTarget() {
        // Given
        given(lineService.findAllLines()).willReturn(Arrays.asList(이호선, 삼호선, 신분당선));
        given(stationService.findById(any())).willReturn(교대역).willReturn(교대역);
        // When & Then
        assertThatThrownBy(() -> pathService.findPath(new PathRequest(교대역.getId(), 교대역.getId())))
                .isInstanceOf(CustomException.class)
                .hasMessage("출발역과 도착역이 동일합니다.");
    }

    @DisplayName("예외 상황 - 출발역과 도착역이 연결이 되어 있지 않은 경우")
    @Test
    void exceptionToFindShortestPathOfUnconnectedSourceAndTarget() {
        // Given
        Station 광교역 = new Station("광교역");
        Station 정자역 = new Station("정자역");
        신분당선 = new Line("신분당선", "bg-red-200", 광교역, 정자역, 1000L);
        given(lineService.findAllLines()).willReturn(Arrays.asList(이호선, 삼호선, 신분당선));
        given(stationService.findById(any())).willReturn(교대역).willReturn(광교역);
        // When & Then
        assertThatThrownBy(() -> pathService.findPath(new PathRequest(교대역.getId(), 광교역.getId())))
                .isInstanceOf(CustomException.class)
                .hasMessage("출발역과 도착역이 연결이 되어 있지 않습니다.");
    }

    @DisplayName("예외 상황 - 존재하지 않은 출발역이나 도착역을 조회 할 경우")
    @Test
    void exceptionToFindNotExistingSourceAndTarget() {
        // Given
        String exceptionMessage = "Station id: 2 존재하지않습니다.";
        given(lineService.findAllLines()).willReturn(Arrays.asList(이호선, 삼호선, 신분당선));
        given(stationService.findById(any())).willReturn(교대역).willThrow(new NoSuchElementException(exceptionMessage));
        // When & Then
        assertThatThrownBy(() -> pathService.findPath(new PathRequest(교대역.getId(), Long.MAX_VALUE)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage(exceptionMessage);
    }
}