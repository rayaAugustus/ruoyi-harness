defineApp({
  page: async () => {
    const result = await harness.call("example.customer.list", { page: 1, size: 20 });
    return page({
      title: "Customer Dashboard",
      children: [
        button({ text: "Refresh", variant: "primary", action: { name: "refresh" } }),
        table({ id: "customers", columns: [
          { key: "name", label: "Name" }, { key: "level", label: "Level" }
        ], rows: result.items })
      ]
    });
  },
  actions: {
    async refresh() { return { refresh: ["page"] }; }
  }
});
