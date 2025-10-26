package ru.tms.user.controller;

@RestController
@RequestMapping("/banks")
@RequiredArgsConstructor
public class BankController {
    
    @PostMapping("/create")
    public ResponseEntity<QuestionBank> createBank(@RequestBody QuestionBank bank) {
}
