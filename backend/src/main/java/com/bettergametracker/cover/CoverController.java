package com.bettergametracker.cover;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/games/{gameId}/cover")
public class CoverController {
    private final CoverService covers;

    public CoverController(CoverService covers) { this.covers = covers; }

    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable UUID gameId) throws IOException {
        byte[] bytes = covers.get(gameId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(CoverStorage.contentType(bytes)))
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> put(@PathVariable UUID gameId, @RequestParam MultipartFile file) throws IOException {
        covers.put(gameId, file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable UUID gameId) throws IOException {
        covers.delete(gameId);
        return ResponseEntity.noContent().build();
    }
}
