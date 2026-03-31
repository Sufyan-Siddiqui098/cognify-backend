"""
Text chunking with overlap
"""
import logging
import re
from typing import List, Tuple

logger = logging.getLogger(__name__)

class ChunkingService:
    def __init__(self, chunk_size: int = 512, overlap: int = 50):
        self.chunk_size = chunk_size
        self.overlap = overlap
    
    def chunk_text(self, text: str, page_map: List[int]) -> List[Tuple[str, int, int]]:
        """
        Chunk text with overlap
        Returns: List of (chunk_text, start_char, end_char)
        """
        logger.info(f"Chunking text ({len(text)} chars)")
        
        # Split into sentences
        sentences = self._split_sentences(text)
        
        chunks = []
        current_chunk = []
        current_length = 0
        start_char = 0
        
        for sentence in sentences:
            sentence_len = len(sentence)
            
            if current_length + sentence_len > self.chunk_size and current_chunk:
                chunk_text = " ".join(current_chunk)
                end_char = start_char + len(chunk_text)
                chunks.append((chunk_text, start_char, end_char))
                
                # Keep last sentence for overlap
                if len(current_chunk) > 1:
                    current_chunk = [current_chunk[-1]]
                    current_length = len(current_chunk[0])
                    start_char = end_char - current_length
                else:
                    current_chunk = []
                    current_length = 0
                    start_char = end_char
            
            current_chunk.append(sentence)
            current_length += sentence_len
        
        # Add final chunk
        if current_chunk:
            chunk_text = " ".join(current_chunk)
            end_char = start_char + len(chunk_text)
            chunks.append((chunk_text, start_char, end_char))
        
        logger.info(f"Created {len(chunks)} chunks")
        return chunks
    
    def _split_sentences(self, text: str) -> List[str]:
        """Simple sentence splitting by paragraphs/newlines/punctuation."""
        sentences = re.split(r'(?<=[.!?\\n])\\s+', text)
        return [s.strip() for s in sentences if s.strip()]
    
    def get_page_number(self, page_map: List[int], start_char: int, end_char: int) -> int:
        """Get most common page number in chunk"""
        if not page_map or end_char > len(page_map):
            return 1
        chunk_pages = page_map[start_char:end_char]
        return max(set(chunk_pages), key=chunk_pages.count) if chunk_pages else 1
