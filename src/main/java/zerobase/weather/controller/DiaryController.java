package zerobase.weather.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import zerobase.weather.domain.Diary;
import zerobase.weather.service.DiaryService;

import java.time.LocalDate;
import java.util.List;

@RestController
public class DiaryController {
    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

//    @ApiOperation(value = "일기의 Text와 날씨를 이용하여 DB에 일기 저장", notes = "상세설명")
    @ApiOperation("일기의 Text와 날씨를 이용하여 DB에 일기 저장")
    // swagger url value는 설명부 이부분이 없으면 함수명이 Default, notes는 api안에 상세설
    @PostMapping("/create/diary")
        // ?뒷부분이 파라미터, 그날의 날짜(date), 일기내용(text)를 받는다
    void createDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody String text) {
        diaryService.createDiary(date, text);
    }

    @ApiOperation("선택한 날짜의 모든 일기 데이터를 가져옵니다")
    @GetMapping("/read/diary")
        // 써놓은일기를 불러오는 과정이기때문에 날짜를 알면 충
    List<Diary> readDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return diaryService.readDiary(date);
    }

    @ApiOperation("선택한 기간중의 모든 일기 데이터를 가져옵니다")
    @GetMapping("/read/diaries")
        // 여러날짜 일기를 범위적으로 가져오기위한 API
    List<Diary> readDiaries(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value = "조회할 기간의 첫번째날", example = "2020-02-02") LocalDate startDate,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value = "조회할 기간의 마지막날", example = "2020-03-04") LocalDate endDate) {
        return diaryService.readDiaries(startDate, endDate);
    }

    @ApiOperation("선택한 날짜와 Text로 일기를 업데이트 해줍니다")
    @PutMapping("/update/diary")
    void updateDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestBody String text) {
        diaryService.updateDiary(date, text);
    }

    @ApiOperation("선택한 날짜의 첫번째 일기를 삭제해줍니다")
    @DeleteMapping("/delete/diary")
    void deleteDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        diaryService.deleteDiary(date);
    }
}
