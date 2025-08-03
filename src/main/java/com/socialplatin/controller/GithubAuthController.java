package com.socialplatin.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class GithubAuthController {

    private static final String CLIENT_ID = "Ov23li766aJxc12btjVV";
    private static final String CLIENT_SECRET = "3f984221e656ba522cedcf287a47c79716f6a361";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/github")
    public ResponseEntity<?> githubAuth(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Código ausente"));
        }

        try {
            // 1. Trocar code por access token
            String tokenUrl = "https://github.com/login/oauth/access_token";

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            Map<String, String> params = new HashMap<>();
            params.put("client_id", CLIENT_ID);
            params.put("client_secret", CLIENT_SECRET);
            params.put("code", code);

            HttpEntity<Map<String, String>> tokenRequest = new HttpEntity<>(params, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            Map tokenBody = tokenResponse.getBody();

            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Token não recebido"));
            }

            String accessToken = (String) tokenBody.get("access_token");

            // 2. Buscar dados do usuário com access token
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    userRequest,
                    Map.class
            );

            Map userBody = userResponse.getBody();

            if (userBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Erro ao obter dados do usuário"));
            }

            // 3. Resposta com dados desejados
            Map<String, Object> response = new HashMap<>();
            response.put("login", userBody.get("login"));
            response.put("name", userBody.get("name"));
            response.put("avatar_url", userBody.get("avatar_url"));
            response.put("email", userBody.get("email"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erro ao autenticar com o GitHub"));
        }
    }
}
