package com.canopy.repository;

import com.canopy.model.DocumentChunk;
import com.canopy.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentOrderByChunkIndex(Document document);
    void deleteByDocument(Document document);

    @Query(value = """
        SELECT dc.*
        FROM document_chunks dc,
        LATERAL (
            SELECT
                SUM(e * q) AS dot_product,
                SQRT(SUM(e * e)) AS norm_doc,
                SQRT(SUM(q * q)) AS norm_query
            FROM
                unnest(dc.embedding) WITH ORDINALITY AS t(e, i)
            JOIN
                unnest(CAST(:queryEmbedding AS float[])) WITH ORDINALITY AS qt(q, j)
                ON i = j
        ) AS sim
        WHERE dc.document_id = :documentId
          AND sim.norm_doc > 0
          AND sim.norm_query > 0
        ORDER BY (sim.dot_product / (sim.norm_doc * sim.norm_query)) DESC NULLS LAST
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findTopKByCosineSimilarity(
        @Param("documentId") Long documentId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("topK") int topK
    );
}
