package com.bittercode.service;

import java.util.List;

import com.bittercode.model.Book;
import com.bittercode.model.StoreException;

public interface BookService {

    public Book getBookById(String bookId) throws StoreException;

    public List<Book> getAllBooks() throws StoreException;

    public List<Book> getBooksPage(int page, int pageSize) throws StoreException;

    public int getBookCount() throws StoreException;

    public List<Book> getBooksByCategory(String category, int page, int pageSize) throws StoreException;

    public int getBookCountByCategory(String category) throws StoreException;

    public List<Book> getBooksByCommaSeperatedBookIds(String commaSeperatedBookIds) throws StoreException;

    public String deleteBookById(String bookId) throws StoreException;

    public String addBook(Book book) throws StoreException;

    public String updateBookQtyById(String bookId, int quantity) throws StoreException;
    
    public String updateBook(Book book) throws StoreException;

}
