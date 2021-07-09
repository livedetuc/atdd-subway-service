package nextstep.subway.path.acceptance;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static nextstep.subway.auth.acceptance.AuthAcceptanceTest.로그인되어있음;
import static nextstep.subway.line.acceptance.LineAcceptanceTest.지하철_노선_등록되어_있음;
import static nextstep.subway.line.acceptance.LineSectionAcceptanceTest.지하철_노선에_지하철역_등록되어_있음;
import static nextstep.subway.member.acceptance.MemberAcceptanceTest.*;
import static nextstep.subway.station.StationAcceptanceTest.지하철역_등록되어_있음;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;


@DisplayName("지하철 경로 조회")
public class GraphAcceptanceTest extends AcceptanceTest {
    private LineResponse 신분당선;
    private LineResponse 이호선;
    private LineResponse 삼호선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 교대역;
    private StationResponse 남부터미널역;

    /**
     * 교대역 -- *2호선 거리:100* -- 강남역
     * |                        |
     * *3호선 거리:8*            *신분당선 거리:30*
     * |                        |
     * 남부터미널역-- *3호선 거리:65* --양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = 지하철역_등록되어_있음("강남역").as(StationResponse.class);
        양재역 = 지하철역_등록되어_있음("양재역").as(StationResponse.class);
        교대역 = 지하철역_등록되어_있음("교대역").as(StationResponse.class);
        남부터미널역 = 지하철역_등록되어_있음("남부터미널역").as(StationResponse.class);

        LineRequest 신분당선요청파라미터 = new LineRequest("신분당선", "bg-red-600", 강남역.getId(), 양재역.getId(), 30, 900);
        LineRequest 이호선요청파라미터 = new LineRequest("이호선", "bg-red-600", 교대역.getId(), 강남역.getId(), 100, 200);
        LineRequest 삼호선요청파라미터 = new LineRequest("삼호선", "bg-red-600", 교대역.getId(), 양재역.getId(), 73, 300);

        신분당선 = 지하철_노선_등록되어_있음(신분당선요청파라미터).as(LineResponse.class);
        이호선 = 지하철_노선_등록되어_있음(이호선요청파라미터).as(LineResponse.class);
        삼호선 = 지하철_노선_등록되어_있음(삼호선요청파라미터).as(LineResponse.class);

        지하철_노선에_지하철역_등록되어_있음(삼호선, 교대역, 남부터미널역, 8);
    }

    @DisplayName("최단경로찾기 - 로그아웃")
    @Test
    public void 경로찾기_성인() throws Exception {
        // when
        ExtractableResponse<Response> response = 지하철_경로찾기_조회_요청(강남역, 남부터미널역, null);
        
        // then
        지하철_경로찾기_조회됨(response);
        최단경로맞음(response, 강남역, 양재역, 남부터미널역);
        최단거리맞음(response, 95);
        요금맞음(response, 3_550);
    }

    @DisplayName("최단경로찾기 - 청소년")
    @Test
    public void 경로찾기_청소년() throws Exception {
        // when
        회원_생성_되어있음(EMAIL, PASSWORD, 15);
        TokenResponse 토큰 = 로그인되어있음(EMAIL, PASSWORD);
        ExtractableResponse<Response> response = 지하철_경로찾기_조회_요청(강남역, 남부터미널역, 토큰);

        // then
        지하철_경로찾기_조회됨(response);
        최단경로맞음(response, 강남역, 양재역, 남부터미널역);
        최단거리맞음(response, 95);
        요금맞음(response, 2_560);
    }

    @DisplayName("최단경로찾기 - 어린이")
    @Test
    public void 경로찾기_어린이() throws Exception {
        // when
        회원_생성_되어있음(EMAIL, PASSWORD, 8);
        TokenResponse 토큰 = 로그인되어있음(EMAIL, PASSWORD);
        ExtractableResponse<Response> response = 지하철_경로찾기_조회_요청(강남역, 남부터미널역, 토큰);

        // then
        지하철_경로찾기_조회됨(response);
        최단경로맞음(response, 강남역, 양재역, 남부터미널역);
        최단거리맞음(response, 95);
        요금맞음(response, 1600);
    }

    private void 요금맞음(ExtractableResponse<Response> response, int expected) {
        PathResponse 최단경로 = response.as(PathResponse.class);
        assertThat(최단경로.getFare()).isEqualTo(expected);
    }

    private void 최단경로맞음(ExtractableResponse<Response> response, StationResponse... expected) {
        PathResponse 최단경로 = response.as(PathResponse.class);
        assertThat(최단경로.getStations()).containsExactly(expected);
    }

    private void 최단거리맞음(ExtractableResponse<Response> response, int expected) {
        PathResponse 최단경로 = response.as(PathResponse.class);
        assertThat(최단경로.getDistance()).isEqualTo(expected);
    }

    private void 지하철_경로찾기_조회됨(ExtractableResponse<Response> response) {
        assertHttpStatus(response, OK);
    }

    private ExtractableResponse<Response> 지하철_경로찾기_조회_요청(StationResponse source, StationResponse target, TokenResponse tokenResponse) {
        if (Objects.isNull(tokenResponse)) {
            return get("/paths?source=" + source.getId() + "&target=" + target.getId());
        }
        return get("/paths?source=" + source.getId() + "&target=" + target.getId(), tokenResponse.getAccessToken());
    }
}
