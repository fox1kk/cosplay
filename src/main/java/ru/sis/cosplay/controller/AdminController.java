package ru.sis.cosplay.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.sis.cosplay.dto.ParseResult;
import ru.sis.cosplay.dto.UiParse;
import ru.sis.cosplay.dto.UiTag;
import ru.sis.cosplay.model.Feed;
import ru.sis.cosplay.service.FeedService;
import ru.sis.cosplay.service.WeiboService;

@Controller
@RequiredArgsConstructor
public class AdminController {
  private final WeiboService weiboService;
  private final FeedService feedService;

  @GetMapping("/")
  public String home(
      Model model,
      @RequestParam("page") Optional<Integer> page,
      @RequestParam("size") Optional<Integer> size,
      @RequestParam("tags") Optional<List<String>> tags) {
    int currentPage = page.orElse(1);
    int pageSize = size.orElse(10);
    Page<Feed> feeds =
        feedService.findPaginated(
            tags, PageRequest.of(currentPage - 1, pageSize, Sort.by("dateCreated").descending()));
    model.addAttribute("feeds", feeds);

    int totalPages = feeds.getTotalPages();
    if (totalPages > 0) {
      List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().toList();
      model.addAttribute("pageNumbers", pageNumbers);
    }

    return "home";
  }

  @GetMapping("/admin/weibo")
  public String weiboForm(Model model) {
    model.addAttribute("uiParse", new UiParse());
    return "weibo";
  }

  @PostMapping("/admin/weibo")
  public String weiboParse(@ModelAttribute UiParse uiParse, Model model) {
    model.addAttribute("parseResult", weiboService.parseUrl(uiParse.getUrl()));
    model.addAttribute("tags", weiboService.getAllTags());
    return "weibo-result";
  }

  @PostMapping("/admin/publish")
  public String weiboPublish(@ModelAttribute ParseResult parseResult, Model model) {
    model.addAttribute("result", weiboService.publish(parseResult, null));
    return "publish";
  }

  @GetMapping("/admin/unique")
  public String unique(Model model) {
    return "unique";
  }

  @PostMapping("/admin/unique")
  public String uniqueCheck(Model model, @RequestParam("files") MultipartFile[] files) {
    model.addAttribute("sim", weiboService.uniqueCheck(files));
    return "unique";
  }

  @GetMapping("/admin/tag")
  public String tag(Model model) {
    model.addAttribute("uiTag", new UiTag());
    return "tag";
  }

  @PostMapping("/admin/tag")
  public String tag(@ModelAttribute UiTag uiTag, Model model) {
    model.addAttribute("uiTag", new UiTag());
    model.addAttribute("result", weiboService.addTag(uiTag.getTag()));
    return "tag";
  }

  @GetMapping("/admin/manual")
  public String manual(Model model) {
    model.addAttribute("parseResult", ParseResult.builder().build());
    model.addAttribute("tags", weiboService.getAllTags());
    return "manual";
  }

  @PostMapping("/admin/manual")
  public String manual(
      Model model,
      @ModelAttribute ParseResult parseResult,
      @RequestParam("files") MultipartFile[] files) {
    model.addAttribute("result", weiboService.publish(parseResult, files));
    return "publish";
  }
}
