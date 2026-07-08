package pizza_cheese.todo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

import pizza_cheese.todo.config.MenuCacheNames;

@Component
public class MenuCacheEvictor {

    @CacheEvict(cacheNames = MenuCacheNames.PIZZAS, allEntries = true)
    public void evictPizzas() {
    }

    @CacheEvict(cacheNames = MenuCacheNames.CATEGORIES, allEntries = true)
    public void evictCategories() {
    }

    @CacheEvict(cacheNames = MenuCacheNames.COMBOS, allEntries = true)
    public void evictCombos() {
    }

    @CacheEvict(cacheNames = MenuCacheNames.TOPPINGS, allEntries = true)
    public void evictToppings() {
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MenuCacheNames.CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = MenuCacheNames.PIZZAS, allEntries = true),
    })
    public void evictCategoriesAndPizzas() {
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = MenuCacheNames.TOPPINGS, allEntries = true),
            @CacheEvict(cacheNames = MenuCacheNames.PIZZAS, allEntries = true),
    })
    public void evictToppingsAndPizzas() {
    }
}
