package com.kazi.kazi.apikey;

import java.util.Set;

public record ApiKeyAuthentication(String owner, Set<String> scopes) {
}
