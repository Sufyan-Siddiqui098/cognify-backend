"""Chunking service using RecursiveCharacterTextSplitter with overlap."""
import logging
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.services.text_sanitizer import sanitize_indexed_text

logger = logging.getLogger(__name__)


class ChunkingService:
    """Splits text into overlapping chunks and keeps character offsets."""

    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", ". ", " ", ""],
            length_function=len,
            is_separator_regex=False,
        )

    def chunk_text(self, text: str, page_map: dict) -> list[tuple[str, int, int]]:
        """
        Split text into overlapping chunks.

        Args:
            text: Full document text
            page_map: Mapping of character offsets to page numbers

        Returns:
            List of (chunk_text, start_char, end_char) tuples
        """
        if not text or not text.strip():
            return []

        chunks = []
        cursor = 0
        split_chunks = self.splitter.split_text(text)

        for split_chunk in split_chunks:
            chunk = sanitize_indexed_text(split_chunk.strip())
            if not chunk:
                continue

            start_idx = text.find(chunk, max(0, cursor - self.chunk_overlap))
            if start_idx == -1:
                start_idx = cursor
            end_idx = start_idx + len(chunk)
            chunks.append((chunk, start_idx, end_idx))
            cursor = end_idx

        logger.info(f"Created {len(chunks)} chunks from {len(text)} characters")
        return chunks

    def get_page_number(self, page_map: dict, start_char: int, end_char: int) -> int:
        """
        Determine the page number for a chunk based on character offsets.
        
        Args:
            page_map: Mapping of character offsets to page numbers
            start_char: Start character offset of the chunk
            end_char: End character offset of the chunk

        Returns:
            Page number (1-indexed)
        """
        if not page_map:
            return 1

        # Find the page that contains the midpoint of the chunk
        mid_char = (start_char + end_char) // 2
        page = 1

        for offset in sorted(page_map.keys()):
            if offset <= mid_char:
                page = page_map[offset]
            else:
                break

        return page
