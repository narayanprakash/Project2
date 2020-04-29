package com.learningreactiveSpring.itemclient.controller;

import com.learningreactiveSpring.itemclient.domain.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ItemClientController {

    WebClient webClient = WebClient.create("http://localhost:8080");

    @GetMapping("/client/retrieve")
    public Flux<Item> getAllItemsUsingRetrieve(){
     return webClient.get().uri("/v1/items")
                .retrieve()
                .bodyToFlux(Item.class)
                .log("Item in client project retrieve");
    }

    @GetMapping("/client/exchange")
    public Flux<Item> getAllItemsUsingExchange(){
        return webClient.get().uri("/v1/items")
                .exchange()
                .flatMapMany(clientResponse -> clientResponse.bodyToFlux(Item.class))
                .log("Item in client project Exchange");
    }

    @GetMapping("/client/retrieve/singleItem")
    public Mono<Item> getAItemUsingRetrieve(){

        String id ="ABC";
        return webClient.get().uri("/v1/items/{id}",id)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Item in client project retrieve");
    }

    @GetMapping("/client/exchange/singleItem")
    public Mono<Item> getAItemUsingExchange(){

        String id ="ABC";
        return webClient.get().uri("/v1/items/{id}",id)
                .exchange()
                .flatMap(item->item.bodyToMono(Item.class))
                .log("Item in client project retrieve");
    }

    @PostMapping("/client/createItem")
    public Mono<Item> createItem(@RequestBody Item item){

        return webClient.post().uri("/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item),Item.class)
                .retrieve()
                .bodyToMono(Item.class)
                .log("Created Item is : ");

    }

    @PutMapping("/client/updateItem/{id}")
    public Mono<Item> updateItem(@PathVariable String id,@RequestBody Item item)
    {
        return webClient.put().uri("/v1/items/{id}",id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(item),Item.class)
                .exchange()
                .flatMap(exchangeItem->exchangeItem.bodyToMono(Item.class))
                .log("exchange Items are :");

    }

    @DeleteMapping("/client/deleteItem/{id}")
    public Mono<Void> deleteItem(@PathVariable String id){
       return webClient.delete().uri("/v1/items/{id}",id).retrieve().bodyToMono(Void.class)
                .log("Deleted Item is");
    }

    @GetMapping("/client/retrieve/error")
    public Flux<Item> errorRetrieve(){
         return webClient.get()
                 .uri("/v1/items/runtimeException")
                 .retrieve()
                 .onStatus(HttpStatus::is5xxServerError,clientResponse -> {
                     Mono<String> errorMono=clientResponse.bodyToMono(String.class);
                     return errorMono.flatMap(errorMsg->{
                         log.error(String.format("The Error Message is : %s",errorMsg));
                         throw new RuntimeException(errorMsg);
                     });

                 }).bodyToFlux(Item.class);
    }

    @GetMapping("/client/exchange/error")
    public Flux<Item> errorExchange(){
        return webClient.get()
                .uri("/v1/items/runtimeException")
                .exchange()
                .flatMapMany(clientResponse -> {
                  if(clientResponse.statusCode().is5xxServerError()){
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errormsg-> {
                                    log.error("Error Message in error Exchange :"+errormsg);
                                    throw new RuntimeException(errormsg);
                                });
                    }
                  else {
                      return clientResponse.bodyToFlux(Item.class);
                  }
                });
    }

}
