package com.kazi.kazi.search;

public class SemanticSearchUnavailableException extends RuntimeException {

    public SemanticSearchUnavailableException() {
        super("Semantic search is disabled; configure Ollama and enable kazi.semantic.enabled");
    }
}
